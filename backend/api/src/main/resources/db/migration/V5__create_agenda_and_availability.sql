-- =====================================================================================
-- Módulo de agenda e disponibilidade dos barbeiros
--
-- Garantias de consistência no banco (última linha de defesa, independentemente do
-- código Java):
--   * Agendamento x Agendamento -> EXCLUDE constraint em schedulings (status ativos)
--   * Bloqueio    x Bloqueio    -> EXCLUDE constraint em schedule_blocks
--   * Agendamento x Bloqueio    -> envolve duas tabelas, então é garantido pela
--                                  serialização da agenda do barbeiro (SELECT ... FOR
--                                  UPDATE na linha de barbers) feita pela aplicação.
--
-- Todos os intervalos são semiabertos [início, fim): 14:00-15:00 e 15:00-16:00 NÃO
-- se sobrepõem.
-- =====================================================================================

-- Necessária para combinar igualdade (barber_id) e sobreposição de intervalos (&&)
-- no mesmo índice GiST. É uma extensão "trusted" (PostgreSQL 13+).
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- -------------------------------------------------------------------------------------
-- 1. Grade semanal de trabalho
--    Uma linha por intervalo de trabalho. Ex.: segunda 08:00-12:00 e 13:00-18:00 = 2 linhas.
--    Dia sem nenhuma linha = folga.
--    day_of_week segue a ISO-8601 / java.time.DayOfWeek: 1 = segunda ... 7 = domingo.
-- -------------------------------------------------------------------------------------
CREATE TABLE barber_working_hours
(
    id          UUID PRIMARY KEY,
    barber_id   UUID NOT NULL,
    day_of_week INT  NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,

    CONSTRAINT chk_working_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_working_hours_interval CHECK (end_time > start_time),

    CONSTRAINT fk_working_hours_barber
        FOREIGN KEY (barber_id)
            REFERENCES barbers (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_working_hours_barber_day ON barber_working_hours (barber_id, day_of_week);

-- Migra os dados do modelo antigo (1 linha por dia com uma única pausa), caso existam.
-- Com pausa válida: gera dois intervalos (início -> início da pausa, fim da pausa -> fim).
INSERT INTO barber_working_hours (id, barber_id, day_of_week, start_time, end_time)
SELECT gen_random_uuid(), barber_id, day_week, start_time, start_pause
FROM availabilities
WHERE active
  AND day_week BETWEEN 1 AND 7
  AND start_time < start_pause
  AND start_pause < end_pause
  AND end_pause < end_time;

INSERT INTO barber_working_hours (id, barber_id, day_of_week, start_time, end_time)
SELECT gen_random_uuid(), barber_id, day_week, end_pause, end_time
FROM availabilities
WHERE active
  AND day_week BETWEEN 1 AND 7
  AND start_time < start_pause
  AND start_pause < end_pause
  AND end_pause < end_time;

-- Sem pausa válida: um único intervalo.
INSERT INTO barber_working_hours (id, barber_id, day_of_week, start_time, end_time)
SELECT gen_random_uuid(), barber_id, day_week, start_time, end_time
FROM availabilities
WHERE active
  AND day_week BETWEEN 1 AND 7
  AND start_time < end_time
  AND NOT (start_time < start_pause AND start_pause < end_pause AND end_pause < end_time);

DROP TABLE availabilities;

-- -------------------------------------------------------------------------------------
-- 2. Bloqueios pontuais (almoço, folga, feriado, compromisso pessoal, saída)
-- -------------------------------------------------------------------------------------
CREATE TABLE schedule_blocks
(
    id          UUID PRIMARY KEY,
    barber_id   UUID        NOT NULL,
    starts_at   TIMESTAMPTZ NOT NULL,
    ends_at     TIMESTAMPTZ NOT NULL,
    reason      VARCHAR(30) NOT NULL CHECK (reason IN
                                            ('LUNCH', 'DAY_OFF', 'HOLIDAY', 'PERSONAL_APPOINTMENT', 'LEAVE')),
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_schedule_block_interval CHECK (ends_at > starts_at),

    CONSTRAINT fk_schedule_block_barber
        FOREIGN KEY (barber_id)
            REFERENCES barbers (id)
            ON DELETE CASCADE,

    -- Bloqueio x Bloqueio: dois bloqueios do mesmo barbeiro nunca se sobrepõem.
    CONSTRAINT excl_schedule_block_overlap
        EXCLUDE USING gist (
            barber_id WITH =,
            tstzrange(starts_at, ends_at, '[)') WITH &&
        )
);

-- -------------------------------------------------------------------------------------
-- 3. Agendamentos: passam a registrar a duração e o fim do atendimento no momento da reserva.
--    ends_at e duration_time são "retratos" do serviço no momento da reserva: alterar a
--    duração do serviço ou o preço depois NÃO altera agendamentos existentes.
-- -------------------------------------------------------------------------------------
ALTER TABLE schedulings
    ADD COLUMN duration_time INT,
    ADD COLUMN ends_at TIMESTAMPTZ;

UPDATE schedulings s
SET duration_time = sv.duration_time,
    ends_at = s.scheduled_at + make_interval(mins => sv.duration_time)
FROM services sv
WHERE sv.id = s.service_id;

ALTER TABLE schedulings
    ALTER COLUMN duration_time SET NOT NULL,
    ALTER COLUMN ends_at SET NOT NULL;

ALTER TABLE schedulings
    ADD CONSTRAINT chk_scheduling_duration CHECK (duration_time > 0),
    ADD CONSTRAINT chk_scheduling_interval CHECK (ends_at > scheduled_at);

-- Agendamento x Agendamento: dois agendamentos que ocupam agenda nunca se sobrepõem
-- para o mesmo barbeiro. CANCELED não ocupa agenda e fica fora da restrição.
-- Deve permanecer em sincronia com SchedulingStatus#occupiesAgenda().
ALTER TABLE schedulings
    ADD CONSTRAINT excl_scheduling_overlap
        EXCLUDE USING gist (
            barber_id WITH =,
            tstzrange(scheduled_at, ends_at, '[)') WITH &&
        ) WHERE (status IN ('PENDING', 'SCHEDULED', 'COMPLETED'));

