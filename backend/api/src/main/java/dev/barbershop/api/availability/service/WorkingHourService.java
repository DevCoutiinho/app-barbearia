package dev.barbershop.api.availability.service;

import dev.barbershop.api.availability.dto.TimeRangeDTO;
import dev.barbershop.api.availability.dto.WeeklyScheduleDTO;
import dev.barbershop.api.availability.dto.WorkingDayDTO;
import dev.barbershop.api.availability.entity.WorkingHour;
import dev.barbershop.api.availability.repository.WorkingHourRepository;
import dev.barbershop.api.barber.entity.Barber;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;

/**
 * Grade semanal padrão do barbeiro (regras 1 e 7).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkingHourService {

    private final AgendaGuard guard;
    private final WorkingHourRepository repository;

    @PreAuthorize("hasRole('BARBER')")
    public WeeklyScheduleDTO findMine(UUID userId) {
        Barber barber = guard.findBarberByUserId(userId);
        return toDTO(repository.findByBarberIdOrderByDayOfWeekAscStartTimeAsc(barber.getId()));
    }

    /**
     * Cria ou define a grade semanal padrão do barbeiro.
     */
    @PreAuthorize("hasRole('BARBER')")
    @Transactional
    public WeeklyScheduleDTO create(UUID userId, WeeklyScheduleDTO dto) {
        return replaceMine(userId, dto);
    }

    /**
     * Substitui a grade inteira (PUT semântico).
     * <p>
     * Regra 7: esta operação atua SOMENTE na tabela barber_working_hours. Agendamentos existentes,
     * inclusive futuros e agora fora da nova grade, permanecem exatamente como estão no banco.
     * A nova grade passa a valer apenas para o cálculo de disponibilidade e para novos agendamentos.
     */
    @PreAuthorize("hasRole('BARBER')")
    @Transactional
    public WeeklyScheduleDTO replaceMine(UUID userId, WeeklyScheduleDTO dto) {
        validate(dto);

        Barber barber = guard.lockAgenda(guard.findBarberByUserId(userId).getId());

        repository.deleteAllByBarberId(barber.getId());

        List<WorkingHour> workingHours = dto.days().stream()
                .flatMap(day -> day.intervals().stream()
                        .map(range -> WorkingHour.of(barber, day.dayOfWeek(), range.start(), range.end())))
                .toList();

        repository.saveAll(workingHours);

        return toDTO(workingHours);
    }

    private void validate(WeeklyScheduleDTO dto) {
        Set<DayOfWeek> seenDays = EnumSet.noneOf(DayOfWeek.class);

        for (WorkingDayDTO day : dto.days()) {
            if (!seenDays.add(day.dayOfWeek())) {
                throw new IllegalArgumentException("O dia " + day.dayOfWeek() + " foi informado mais de uma vez");
            }

            List<TimeRangeDTO> sorted = day.intervals().stream()
                    .sorted(Comparator.comparing(TimeRangeDTO::start))
                    .toList();

            TimeRangeDTO previous = null;
            for (TimeRangeDTO range : sorted) {
                if (!range.end().isAfter(range.start())) {
                    throw new IllegalArgumentException(
                            "Intervalo inválido em " + day.dayOfWeek() + ": o horário final deve ser posterior ao inicial");
                }
                if (previous != null && range.start().isBefore(previous.end())) {
                    throw new IllegalArgumentException(
                            "Os intervalos de " + day.dayOfWeek() + " não podem se sobrepor");
                }
                previous = range;
            }
        }
    }

    /** Retorna sempre os 7 dias; dia sem intervalos = folga. */
    private WeeklyScheduleDTO toDTO(List<WorkingHour> workingHours) {
        Map<DayOfWeek, List<TimeRangeDTO>> byDay = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            byDay.put(day, new ArrayList<>());
        }

        workingHours.stream()
                .sorted(Comparator.comparing(WorkingHour::getDayOfWeek).thenComparing(WorkingHour::getStartTime))
                .forEach(wh -> byDay.get(wh.getDayOfWeek()).add(new TimeRangeDTO(wh.getStartTime(), wh.getEndTime())));

        List<WorkingDayDTO> days = byDay.entrySet().stream()
                .map(entry -> new WorkingDayDTO(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();

        return new WeeklyScheduleDTO(days);
    }
}

