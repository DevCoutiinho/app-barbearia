package dev.barbershop.api.availability.entity;

import dev.barbershop.api.barber.entity.Barber;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Um intervalo da grade semanal padrão do barbeiro.
 * Ex.: segunda 08:00-12:00 e segunda 13:00-18:00 são duas linhas. Dia sem linhas = folga.
 * <p>
 * A grade é usada APENAS para calcular a disponibilidade de novos agendamentos.
 * Alterá-la nunca modifica agendamentos já existentes.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"barber"})
@Table(name = "barber_working_hours")
public class WorkingHour {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @Convert(converter = DayOfWeekConverter.class)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public static WorkingHour of(Barber barber, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        Objects.requireNonNull(barber, "O barbeiro é obrigatório");
        Objects.requireNonNull(dayOfWeek, "O dia da semana é obrigatório");
        Objects.requireNonNull(startTime, "O horário inicial é obrigatório");
        Objects.requireNonNull(endTime, "O horário final é obrigatório");

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("O horário final deve ser posterior ao horário inicial");
        }

        WorkingHour workingHour = new WorkingHour();
        workingHour.barber = barber;
        workingHour.dayOfWeek = dayOfWeek;
        workingHour.startTime = startTime;
        workingHour.endTime = endTime;
        return workingHour;
    }
}

