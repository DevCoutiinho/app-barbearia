package dev.barbershop.api.scheduling.service;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.availability.service.AgendaGuard;
import dev.barbershop.api.barberservice.entity.BarberService;
import dev.barbershop.api.barberservice.repository.BarberServiceRepository;
import dev.barbershop.api.client.entity.Client;
import dev.barbershop.api.client.repository.ClientRepository;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.config.scheduling.SchedulingProperties;
import dev.barbershop.api.scheduling.dto.SchedulingCreateDTO;
import dev.barbershop.api.scheduling.dto.SchedulingResponseDTO;
import dev.barbershop.api.scheduling.entity.Scheduling;
import dev.barbershop.api.scheduling.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchedulingService {

    private final AgendaGuard guard;
    private final SchedulingRepository repository;
    private final ClientRepository clientRepository;
    private final BarberServiceRepository barberServiceRepository;
    private final SchedulingProperties properties;
    private final Clock clock;

    /**
     * Cria um agendamento garantindo, sob o lock da agenda do barbeiro, que o intervalo inteiro:
     * <ul>
     *     <li>cabe na grade de trabalho VIGENTE;</li>
     *     <li>não cruza nenhum bloqueio;</li>
     *     <li>não cruza nenhum agendamento que ocupa a agenda.</li>
     * </ul>
     * Duas requisições simultâneas para o mesmo horário são serializadas pelo lock: a segunda só valida
     * depois que a primeira confirmou e, portanto, recebe 409. A EXCLUDE constraint do banco é a
     * garantia final.
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public SchedulingResponseDTO create(UUID userId, SchedulingCreateDTO dto) {
        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Não existe cliente associado ao usuário informado"));

        BarberService service = barberServiceRepository.findById(dto.serviceId())
                .filter(BarberService::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado ou inativo"));

        Instant start = dto.startAt().toInstant();
        validateBookingWindow(start);

        TimeInterval period = new TimeInterval(start, start.plus(Duration.ofMinutes(service.getDurationTime())));
        UUID barberId = service.getBarber().getId();

        guard.lockAgenda(barberId);

        guard.assertWithinWorkingHours(barberId, period);
        guard.assertSlotNotBlocked(barberId, period);
        guard.assertSlotFreeOfBookings(barberId, period);

        Scheduling scheduling = guard.persistGuarded(
                () -> repository.saveAndFlush(Scheduling.book(client, service, period))
        );

        return toDTO(scheduling);
    }

    /**
     * Cancelar apenas libera tempo na agenda, então não precisa do lock. Pode ser feito pelo cliente
     * dono do agendamento ou pelo barbeiro responsável.
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public SchedulingResponseDTO cancel(UUID userId, UUID schedulingId) {
        Scheduling scheduling = repository.findById(schedulingId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        boolean isOwnerClient = scheduling.getClient().getUser().getId().equals(userId);
        boolean isOwnerBarber = scheduling.getBarber().getUser().getId().equals(userId);

        if (!isOwnerClient && !isOwnerBarber) {
            throw new AccessDeniedException("Agendamento pertence a outro usuário");
        }

        scheduling.cancel();

        return toDTO(scheduling);
    }

    private void validateBookingWindow(Instant start) {
        if (!start.isAfter(clock.instant())) {
            throw new IllegalArgumentException("O horário de início deve estar no futuro");
        }

        ZoneId zone = guard.zone();
        LocalDate lastAllowedDay = LocalDate.now(clock.withZone(zone)).plusDays(properties.maxDaysAhead());

        if (start.atZone(zone).toLocalDate().isAfter(lastAllowedDay)) {
            throw new IllegalArgumentException(
                    "Agendamentos só podem ser feitos para os próximos " + properties.maxDaysAhead() + " dias");
        }
    }

    private SchedulingResponseDTO toDTO(Scheduling scheduling) {
        ZoneId zone = guard.zone();
        return new SchedulingResponseDTO(
                scheduling.getId(),
                scheduling.getBarber().getId(),
                scheduling.getService().getId(),
                scheduling.getService().getName(),
                scheduling.getScheduledAt().atZone(zone).toOffsetDateTime(),
                scheduling.getEndsAt().atZone(zone).toOffsetDateTime(),
                scheduling.getStatus(),
                scheduling.getFinalPrice()
        );
    }
}

