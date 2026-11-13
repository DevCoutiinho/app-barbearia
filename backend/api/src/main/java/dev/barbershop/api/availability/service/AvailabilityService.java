package dev.barbershop.api.availability.service;

import dev.barbershop.api.availability.dto.AvailableSlotDTO;
import dev.barbershop.api.availability.entity.Availability;
import dev.barbershop.api.availability.repository.AvailabilityRepository;
import dev.barbershop.api.barber.repository.BarberRepository;
import dev.barbershop.api.barberservice.entity.BarberService;
import dev.barbershop.api.barberservice.repository.BarberServiceRepository;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.scheduling.entity.Scheduling;
import dev.barbershop.api.scheduling.enums.SchedulingStatus;
import dev.barbershop.api.scheduling.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private final AvailabilityRepository repository;
    private final BarberRepository barberRepository;
    private final BarberServiceRepository serviceRepository;
    private final SchedulingRepository schedulingRepository;

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("America/Sao_Paulo");

    public List<AvailableSlotDTO> findAvailableSlots(
            UUID barberId,
            UUID serviceId,
            LocalDate date
    ) {

        // Busca o barbeiro
        barberRepository.findById(barberId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Barbeiro não encontrado"
                        ));

        BarberService service = serviceRepository
                .findByIdAndBarberId(serviceId, barberId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Serviço não encontrado para este barbeiro"
                        ));

        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new IllegalArgumentException(
                    "Este serviço não está disponível"
            );
        }

        int dayWeek = date.getDayOfWeek().getValue();

        List<Availability> shifts = repository
                .findAvailableByBarberAndDay(barberId, dayWeek);

        if (shifts.isEmpty()) {
            return List.of();
        }

        if (date.isBefore(LocalDate.now(BUSINESS_ZONE))) {
            return List.of();
        }

        Instant dayStart = date.atStartOfDay(BUSINESS_ZONE)
                .toInstant();

        Instant dayEnd = date.plusDays(1)
                .atStartOfDay(BUSINESS_ZONE)
                .toInstant();

        List<Scheduling> bookings =
                schedulingRepository.findBarberBookings(
                    barberId,
                    dayEnd,
                    SchedulingStatus.CANCELED
                );

        Integer duration = service.getDurationTime();

        if (duration == null || duration <= 0) {
            throw new IllegalArgumentException(
                    "A duração do serviço deve ser maior que zero"
            );
        }

        List<AvailableSlotDTO> result = new ArrayList<>();

        Instant now = Instant.now();

        for (Availability shift : shifts) {

            LocalTime cursor = shift.getStartTime();

            while (!cursor.plusMinutes(duration)
                    .isAfter(shift.getEndTime())) {

                LocalTime slotEnd = cursor.plusMinutes(duration);

                boolean intersectsPause =
                        shift.getStartPause() != null
                        && shift.getEndPause() != null
                        && cursor.isBefore(shift.getEndPause())
                        && slotEnd.isAfter(shift.getStartPause());

                if (intersectsPause) {
                    cursor = shift.getEndPause();
                    continue;
                }

                Instant start = date.atTime(cursor)
                        .atZone(BUSINESS_ZONE)
                        .toInstant();

                Instant end = date.atTime(slotEnd)
                        .atZone(BUSINESS_ZONE)
                        .toInstant();

                if (!start.isAfter(now)) {
                    cursor = slotEnd;
                    continue;
                }

                Scheduling conflict = bookings.stream()
                        .filter(booking -> {

                            Instant bookingStart =
                                    booking.getScheduledAt();

                            Instant bookingEnd = bookingStart.plus(
                                    Duration.ofMinutes(
                                            booking.getService()
                                                    .getDurationTime()
                                    )
                            );

                            return start.isBefore(bookingEnd)
                                    && end.isAfter(bookingStart);
                        })
                        .findFirst()
                        .orElse(null);

                if (conflict != null) {

                    Instant conflictEnd =
                            conflict.getScheduledAt().plus(
                                    Duration.ofMinutes(
                                            conflict.getService()
                                                    .getDurationTime()
                                    )
                            );

                    LocalDateTime nextAvailable =
                            LocalDateTime.ofInstant(
                                    conflictEnd,
                                    BUSINESS_ZONE
                            );

                    if (!nextAvailable.toLocalDate().equals(date)) {
                        break;
                    }

                    cursor = nextAvailable.toLocalTime();

                } else {

                    result.add(
                            new AvailableSlotDTO(cursor, slotEnd)
                    );

                    cursor = slotEnd;
                }
            }
        }

        return result.stream()
                .distinct()
                .sorted((a, b) ->
                        a.startTime().compareTo(b.startTime())
                )
                .toList();
    }
}
