package dev.barbershop.api.availability.service;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.availability.dto.AvailableSlotDTO;
import dev.barbershop.api.availability.repository.ScheduleBlockRepository;
import dev.barbershop.api.barberservice.entity.BarberService;
import dev.barbershop.api.barberservice.repository.BarberServiceRepository;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.config.scheduling.SchedulingProperties;
import dev.barbershop.api.scheduling.enums.SchedulingStatus;
import dev.barbershop.api.scheduling.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Cálculo de disponibilidade exibida ao cliente (regra 3):
 * <pre>
 *     Grade de trabalho - Bloqueios - Agendamentos que ocupam a agenda = intervalos livres
 * </pre>
 * Um horário inicial só é oferecido se o intervalo INTEIRO [início, início + duração do serviço)
 * couber em um intervalo livre.
 * <p>
 * Esta consulta é apenas informativa. A garantia real acontece em
 * {@code SchedulingService#create}, que revalida tudo sob o lock da agenda, pois o horário pode
 * ser reservado por outra pessoa entre a consulta e a confirmação.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private final AgendaGuard guard;
    private final BarberServiceRepository barberServiceRepository;
    private final ScheduleBlockRepository blockRepository;
    private final SchedulingRepository schedulingRepository;
    private final SchedulingProperties properties;
    private final Clock clock;

    public List<AvailableSlotDTO> findAvailableSlots(UUID barberId, UUID serviceId, LocalDate date) {
        ZoneId zone = guard.zone();
        LocalDate today = LocalDate.now(clock.withZone(zone));

        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Não é possível consultar disponibilidade de datas passadas");
        }
        if (date.isAfter(today.plusDays(properties.maxDaysAhead()))) {
            throw new IllegalArgumentException(
                    "A disponibilidade só pode ser consultada para os próximos " + properties.maxDaysAhead() + " dias");
        }

        BarberService service = barberServiceRepository.findByIdAndBarberId(serviceId, barberId)
                .filter(BarberService::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado ou inativo para este barbeiro"));

        Duration duration = Duration.ofMinutes(service.getDurationTime());

        // 1. Grade de trabalho do dia da semana, já em instantes absolutos
        List<TimeInterval> work = guard.workIntervalsOn(barberId, date);
        if (work.isEmpty()) {
            return List.of(); // folga
        }

        Instant windowStart = work.getFirst().start();
        Instant windowEnd = work.stream().map(TimeInterval::end).max(Comparator.naturalOrder()).orElseThrow();

        // 2. Tudo que ocupa a agenda nessa janela: bloqueios + agendamentos ativos
        List<TimeInterval> busy = new ArrayList<>();
        busy.addAll(blockRepository.findBlockedIntervals(barberId, windowStart, windowEnd));
        busy.addAll(schedulingRepository.findOccupiedIntervals(
                barberId, windowStart, windowEnd, SchedulingStatus.occupyingStatuses()));

        // 3. Grade - ocupados = livres
        List<TimeInterval> free = TimeInterval.subtract(work, busy);

        // 4. Gera candidatos a cada "slotInterval" a partir do início de cada intervalo de trabalho
        //    e mantém só os que cabem inteiros em um intervalo livre e ainda não passaram.
        Instant now = clock.instant();
        Duration step = properties.slotInterval();
        List<AvailableSlotDTO> slots = new ArrayList<>();

        for (TimeInterval workInterval : work) {
            for (Instant start = workInterval.start();
                 !start.plus(duration).isAfter(workInterval.end());
                 start = start.plus(step)) {

                if (start.isBefore(now)) {
                    continue;
                }

                TimeInterval candidate = new TimeInterval(start, start.plus(duration));

                if (free.stream().anyMatch(interval -> interval.contains(candidate))) {
                    slots.add(new AvailableSlotDTO(
                            candidate.start().atZone(zone).toOffsetDateTime(),
                            candidate.end().atZone(zone).toOffsetDateTime()
                    ));
                }
            }
        }

        return slots;
    }
}
