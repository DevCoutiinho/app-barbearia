package dev.barbershop.api.scheduling.entity;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.barber.entity.Barber;
import dev.barbershop.api.barberservice.entity.BarberService;
import dev.barbershop.api.client.entity.Client;
import dev.barbershop.api.common.auditing.Auditable;
import dev.barbershop.api.scheduling.enums.SchedulingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(
        onlyExplicitlyIncluded = true,
        callSuper = false
)
@ToString(exclude = {"barber", "client", "service"})
@Table(name = "schedulings")
public class Scheduling extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private BarberService service;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    /**
     * Fim do atendimento, calculado no momento da reserva (scheduledAt + duração do serviço).
     * É um "retrato": alterar depois a duração do serviço ou a grade do barbeiro não altera
     * o período já reservado.
     */
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SchedulingStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finalPrice;

    public static Scheduling book(Client client, BarberService service, TimeInterval period) {
        Objects.requireNonNull(client, "O cliente é obrigatório");
        Objects.requireNonNull(service, "O serviço é obrigatório");
        Objects.requireNonNull(period, "O período é obrigatório");

        Scheduling scheduling = new Scheduling();
        scheduling.client = client;
        scheduling.service = service;
        scheduling.barber = service.getBarber();
        scheduling.scheduledAt = period.start();
        scheduling.endsAt = period.end();
        scheduling.finalPrice = service.getPrice();
        scheduling.status = SchedulingStatus.SCHEDULED;
        return scheduling;
    }

    public TimeInterval period() {
        return new TimeInterval(scheduledAt, endsAt);
    }

    public void cancel() {
        if (!status.isCancelable()) {
            throw new IllegalStateException("Apenas agendamentos pendentes ou confirmados podem ser cancelados");
        }
        this.status = SchedulingStatus.CANCELED;
    }
}
