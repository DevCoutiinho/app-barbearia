package dev.barbershop.api.scheduling.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum SchedulingStatus {
    /** Cancelado pelo cliente ou pelo barbeiro. Libera o horário. */
    CANCELED(false),
    /** Atendimento realizado. Continua ocupando o período (histórico consistente). */
    COMPLETED(true),
    /** Agendamento confirmado. Ocupa a agenda. */
    SCHEDULED(true),
    /** Aguardando confirmação. Já reserva o horário para evitar dupla reserva. */
    PENDING(true);

    private final boolean occupiesAgenda;

    SchedulingStatus(boolean occupiesAgenda) {
        this.occupiesAgenda = occupiesAgenda;
    }

    /**
     * Indica se um agendamento neste status impede novos agendamentos/bloqueios no mesmo período.
     * <p>
     * IMPORTANTE: deve permanecer em sincronia com o predicado da constraint
     * {@code excl_scheduling_overlap} (migration V5).
     */
    public boolean occupiesAgenda() {
        return occupiesAgenda;
    }

    public boolean isCancelable() {
        return this == PENDING || this == SCHEDULED;
    }

    public static Set<SchedulingStatus> occupyingStatuses() {
        return Arrays.stream(values())
                .filter(SchedulingStatus::occupiesAgenda)
                .collect(Collectors.toUnmodifiableSet());
    }
}
