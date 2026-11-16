package dev.barbershop.api.availability.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TimeInterval - Regras de intervalo e sobreposição")
class TimeIntervalTest {

    private final Instant t1 = Instant.parse("2026-11-20T10:00:00Z");
    private final Instant t2 = Instant.parse("2026-11-20T11:00:00Z");
    private final Instant t3 = Instant.parse("2026-11-20T12:00:00Z");
    private final Instant t4 = Instant.parse("2026-11-20T13:00:00Z");

    @Test
    @DisplayName("Deve rejeitar intervalo onde fim é menor ou igual ao início")
    void shouldRejectInvalidInterval() {
        assertThrows(IllegalArgumentException.class, () -> new TimeInterval(t2, t1));
        assertThrows(IllegalArgumentException.class, () -> new TimeInterval(t1, t1));
    }

    @Nested
    @DisplayName("Regra 4 e 5: Detecção de sobreposição (semiaberto [start, end))")
    class OverlapTests {

        @Test
        @DisplayName("Intervalos adjacentes (ex: 10:00-11:00 e 11:00-12:00) NÃO devem se sobrepor")
        void adjacentIntervalsShouldNotOverlap() {
            TimeInterval a = new TimeInterval(t1, t2);
            TimeInterval b = new TimeInterval(t2, t3);

            assertFalse(a.overlaps(b), "10-11 não deve sobrepor 11-12");
            assertFalse(b.overlaps(a), "11-12 não deve sobrepor 10-11");
        }

        @Test
        @DisplayName("Sobreposição parcial à direita (ex: 10:00-12:00 e 11:00-13:00)")
        void partialOverlapRight() {
            TimeInterval a = new TimeInterval(t1, t3);
            TimeInterval b = new TimeInterval(t2, t4);

            assertTrue(a.overlaps(b));
            assertTrue(b.overlaps(a));
        }

        @Test
        @DisplayName("Um intervalo contido completamente dentro de outro")
        void containedOverlap() {
            TimeInterval outer = new TimeInterval(t1, t4); // 10:00 - 13:00
            TimeInterval inner = new TimeInterval(t2, t3); // 11:00 - 12:00

            assertTrue(outer.overlaps(inner));
            assertTrue(inner.overlaps(outer));
            assertTrue(outer.contains(inner));
            assertFalse(inner.contains(outer));
        }

        @Test
        @DisplayName("Intervalos idênticos devem se sobrepor")
        void identicalIntervalsOverlap() {
            TimeInterval a = new TimeInterval(t1, t2);
            TimeInterval b = new TimeInterval(t1, t2);

            assertTrue(a.overlaps(b));
            assertTrue(a.contains(b));
        }

        @Test
        @DisplayName("Intervalos completamente separados não devem se sobrepor")
        void completelyDisjointIntervals() {
            TimeInterval a = new TimeInterval(t1, t2); // 10:00 - 11:00
            TimeInterval b = new TimeInterval(t3, t4); // 12:00 - 13:00

            assertFalse(a.overlaps(b));
            assertFalse(b.overlaps(a));
        }
    }

    @Nested
    @DisplayName("Regra 3: Subtração (Grade - Ocupados)")
    class SubtractionTests {

        @Test
        @DisplayName("Subtrair bloqueio no meio deve dividir o intervalo em dois livres")
        void shouldSplitIntervalWhenOccupiedInMiddle() {
            TimeInterval base = new TimeInterval(t1, t4); // 10:00 - 13:00
            TimeInterval occupied = new TimeInterval(t2, t3); // 11:00 - 12:00

            List<TimeInterval> free = TimeInterval.subtract(List.of(base), List.of(occupied));

            assertEquals(2, free.size());
            assertEquals(new TimeInterval(t1, t2), free.get(0)); // 10:00 - 11:00
            assertEquals(new TimeInterval(t3, t4), free.get(1)); // 12:00 - 13:00
        }

        @Test
        @DisplayName("Subtrair ocupação total deve resultar em lista vazia")
        void shouldReturnEmptyWhenFullyOccupied() {
            TimeInterval base = new TimeInterval(t2, t3);
            TimeInterval occupied = new TimeInterval(t1, t4);

            List<TimeInterval> free = TimeInterval.subtract(List.of(base), List.of(occupied));
            assertTrue(free.isEmpty());
        }

        @Test
        @DisplayName("Quando não há ocupação, o intervalo base deve permanecer intacto")
        void shouldKeepBaseWhenNoOccupations() {
            TimeInterval base = new TimeInterval(t1, t2);
            List<TimeInterval> free = TimeInterval.subtract(List.of(base), List.of());

            assertEquals(1, free.size());
            assertEquals(base, free.get(0));
        }

        @Test
        @DisplayName("Subtrair agendamento de 30 min (10:00-10:30) não deve deixar livre nenhum candidato menor dentro dele (ex: 10:00-10:15)")
        void shorterServiceInsideOccupiedShouldNotFitInFreeIntervals() {
            Instant t0800 = Instant.parse("2026-11-14T08:00:00Z");
            Instant t1000 = Instant.parse("2026-11-14T10:00:00Z");
            Instant t1015 = Instant.parse("2026-11-14T10:15:00Z");
            Instant t1030 = Instant.parse("2026-11-14T10:30:00Z");
            Instant t1800 = Instant.parse("2026-11-14T18:00:00Z");

            TimeInterval work = new TimeInterval(t0800, t1800);
            TimeInterval occupied30min = new TimeInterval(t1000, t1030);

            List<TimeInterval> free = TimeInterval.subtract(List.of(work), List.of(occupied30min));

            assertEquals(2, free.size());
            assertEquals(new TimeInterval(t0800, t1000), free.get(0));
            assertEquals(new TimeInterval(t1030, t1800), free.get(1));

            TimeInterval candidate15min = new TimeInterval(t1000, t1015);
            assertFalse(free.stream().anyMatch(interval -> interval.contains(candidate15min)),
                    "O candidato de 15 minutos às 10:00 não pode caber em nenhum intervalo livre");

            TimeInterval candidateInside1015 = new TimeInterval(t1015, t1030);
            assertFalse(free.stream().anyMatch(interval -> interval.contains(candidateInside1015)),
                    "O candidato de 15 minutos às 10:15 não pode caber em nenhum intervalo livre");

            TimeInterval candidateAfter1030 = new TimeInterval(t1030, Instant.parse("2026-11-14T10:45:00Z"));
            assertTrue(free.stream().anyMatch(interval -> interval.contains(candidateAfter1030)),
                    "O candidato de 15 minutos às 10:30 deve caber no intervalo livre da tarde");
        }
    }
}

