package dev.barbershop.api.availability.entity;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.availability.enums.BlockReason;
import dev.barbershop.api.barber.entity.Barber;
import dev.barbershop.api.common.auditing.Creatable;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Bloqueio pontual da agenda (almoço, folga, feriado, compromisso pessoal, saída).
 * Período semiaberto [startsAt, endsAt).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(exclude = {"barber"})
@Table(name = "schedule_blocks")
public class ScheduleBlock extends Creatable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BlockReason reason;

    @Column(length = 255)
    private String description;

    public static ScheduleBlock of(Barber barber, TimeInterval period, BlockReason reason, String description) {
        ScheduleBlock block = new ScheduleBlock();
        block.barber = Objects.requireNonNull(barber, "O barbeiro é obrigatório");
        block.startsAt = Objects.requireNonNull(period, "O período é obrigatório").start();
        block.endsAt = period.end();
        block.reason = Objects.requireNonNull(reason, "O motivo do bloqueio é obrigatório");
        block.description = description;
        return block;
    }

    public TimeInterval period() {
        return new TimeInterval(startsAt, endsAt);
    }
}

