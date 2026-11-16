package dev.barbershop.api.availability.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Intervalo de tempo semiaberto [start, end).
 * <p>
 * Por ser semiaberto, intervalos adjacentes (14:00-15:00 e 15:00-16:00) NÃO se sobrepõem,
 * o que permite agendamentos "colados" um no outro. A mesma semântica é usada nas
 * EXCLUDE constraints do banco (tstzrange '[)').
 */
public record TimeInterval(Instant start, Instant end) {

    public TimeInterval {
        Objects.requireNonNull(start, "O início do intervalo é obrigatório");
        Objects.requireNonNull(end, "O fim do intervalo é obrigatório");

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("O horário final deve ser posterior ao horário inicial");
        }
    }

    /**
     * Qualquer tipo de sobreposição: parcial no início, parcial no fim, um contendo o outro
     * ou intervalos idênticos. Equivale a {@code a.start < b.end AND a.end > b.start}.
     */
    public boolean overlaps(TimeInterval other) {
        return start.isBefore(other.end) && end.isAfter(other.start);
    }

    public boolean contains(TimeInterval other) {
        return !other.start.isBefore(start) && !other.end.isAfter(end);
    }

    /**
     * Subtrai os intervalos ocupados dos intervalos base.
     * Ex.: base [08:00, 12:00) - ocupados [09:00, 10:00) = [08:00, 09:00), [10:00, 12:00)
     */
    public static List<TimeInterval> subtract(List<TimeInterval> base, List<TimeInterval> busy) {
        List<TimeInterval> sortedBusy = busy.stream()
                .sorted(Comparator.comparing(TimeInterval::start))
                .toList();

        List<TimeInterval> result = new ArrayList<>();

        for (TimeInterval interval : base) {
            Instant cursor = interval.start;

            for (TimeInterval occupied : sortedBusy) {
                if (!occupied.overlaps(interval)) {
                    continue;
                }
                if (occupied.start.isAfter(cursor)) {
                    result.add(new TimeInterval(cursor, occupied.start));
                }
                if (occupied.end.isAfter(cursor)) {
                    cursor = occupied.end;
                }
                if (!cursor.isBefore(interval.end)) {
                    break;
                }
            }

            if (cursor.isBefore(interval.end)) {
                result.add(new TimeInterval(cursor, interval.end));
            }
        }

        return result;
    }
}

