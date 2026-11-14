-- =====================================================================================
-- Histórico e auditoria de remarcações de agendamentos
-- =====================================================================================

ALTER TABLE schedulings
    ADD COLUMN previous_scheduled_at TIMESTAMPTZ,
    ADD COLUMN previous_ends_at TIMESTAMPTZ,
    ADD COLUMN reschedule_count INT NOT NULL DEFAULT 0;

