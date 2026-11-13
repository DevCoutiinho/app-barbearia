package dev.barbershop.api.scheduling.service;

import dev.barbershop.api.availability.dto.AvailableSlotDTO;
import dev.barbershop.api.availability.service.AvailabilityService;
import dev.barbershop.api.barber.entity.Barber;
import dev.barbershop.api.barber.repository.BarberRepository;
import dev.barbershop.api.barberservice.entity.BarberService;
import dev.barbershop.api.barberservice.repository.BarberServiceRepository;
import dev.barbershop.api.client.entity.Client;
import dev.barbershop.api.client.repository.ClientRepository;
import dev.barbershop.api.common.exception.ResourceConflictException;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.scheduling.dto.SchedulingResponseDTO;
import dev.barbershop.api.scheduling.entity.Scheduling;
import dev.barbershop.api.scheduling.enums.SchedulingStatus;
import dev.barbershop.api.scheduling.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final SchedulingRepository schedulingRepository;
    private final BarberRepository barberRepository;
    private final BarberServiceRepository serviceRepository;
    private final ClientRepository clientRepository;
    private final AvailabilityService availabilityService;

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("America/Sao_Paulo");

    @Transactional
    public Scheduling create(
            UUID userId,
            UUID barberId,
            UUID serviceId,
            Instant scheduledAt
    ) {
        if (scheduledAt == null) {
            throw new IllegalArgumentException(
                    "A data e o horário são obrigatórios"
            );
        }

        Barber barber = barberRepository.findByIdWithLock(barberId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Barbeiro não encontrado"));

        Client client = clientRepository.findClientByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cliente não encontrado"));

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

        LocalDateTime requestedDateTime =
                LocalDateTime.ofInstant(scheduledAt, BUSINESS_ZONE);

        List<AvailableSlotDTO> availableSlots =
                availabilityService.findAvailableSlots(
                        barberId,
                        serviceId,
                        requestedDateTime.toLocalDate()
                );

        boolean available = availableSlots.stream()
                .anyMatch(slot ->
                        slot.startTime().equals(requestedDateTime.toLocalTime())
                );

        if (!available) {
            throw new ResourceConflictException(
                    "Este horário acabou de ser reservado por outra pessoa. Por favor, escolha outro."
            );
        }

        Scheduling scheduling = Scheduling.builder()
                .barber(barber)
                .client(client)
                .service(service)
                .scheduledAt(scheduledAt)
                .status(SchedulingStatus.PENDING)
                .finalPrice(service.getPrice())
                .build();

        return schedulingRepository.save(scheduling);
    }

    @Transactional(readOnly = true)
    public List<SchedulingResponseDTO> findMySchedulings(UUID userId) {

        Client client = clientRepository.findClientByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cliente não encontrado"));

        List<Scheduling> schedulings =
                schedulingRepository.findClientSchedulings(client.getId());

        return schedulings.stream()
                .map(scheduling -> new SchedulingResponseDTO(
                        scheduling.getId(),
                        scheduling.getService().getName(),
                        scheduling.getBarber().getUser().getName(),
                        scheduling.getScheduledAt(),
                        scheduling.getService().getDurationTime(),
                        scheduling.getFinalPrice(),
                        scheduling.getStatus()
                ))
                .toList();
    }
}
