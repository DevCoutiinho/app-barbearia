package dev.barbershop.api.scheduling.service;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.availability.service.AgendaGuard;
import dev.barbershop.api.barber.entity.Barber;
import dev.barbershop.api.barberservice.entity.BarberService;
import dev.barbershop.api.barberservice.repository.BarberServiceRepository;
import dev.barbershop.api.client.entity.Client;
import dev.barbershop.api.client.repository.ClientRepository;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.config.scheduling.SchedulingProperties;
import dev.barbershop.api.scheduling.dto.BarberAppointmentHistoryDTO;
import dev.barbershop.api.scheduling.dto.BarberSchedulingFilterDTO;
import dev.barbershop.api.scheduling.dto.SchedulingCreateDTO;
import dev.barbershop.api.scheduling.dto.SchedulingResponseDTO;
import dev.barbershop.api.scheduling.entity.Scheduling;
import dev.barbershop.api.scheduling.repository.SchedulingRepository;
import dev.barbershop.api.scheduling.specification.SchedulingSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    @PreAuthorize("isAuthenticated()")
    public java.util.List<SchedulingResponseDTO> findMySchedulings(UUID userId) {
        return repository.findByClientUserIdOrderByScheduledAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

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

        if (dto.barberId() != null && !service.getBarber().getId().equals(dto.barberId())) {
            throw new IllegalArgumentException("O serviço selecionado não pertence ao barbeiro informado");
        }

        if (!service.getBarber().getUser().isActive()) {
            throw new IllegalStateException("O barbeiro selecionado está inativo no sistema");
        }

        Instant start = dto.startAt().toInstant();
        validateBookingWindow(start);

        TimeInterval period = new TimeInterval(start, start.plus(Duration.ofMinutes(service.getDurationTime())));
        UUID barberId = service.getBarber().getId();

        guard.lockAgenda(barberId);

        guard.assertWithinWorkingHours(barberId, period);
        guard.assertSlotNotBlocked(barberId, period);
        guard.assertSlotFreeOfBookings(barberId, period);
        guard.assertClientFreeOfBookings(client.getId(), period);

        Scheduling scheduling = guard.persistGuarded(
                () -> repository.saveAndFlush(Scheduling.book(client, service, period))
        );

        return toDTO(scheduling);
    }

    /**
     * Cancelar apenas libera tempo na agenda, então não precisa do lock. Pode ser feito pelo cliente
     * dono do agendamento ou pelo barbeiro responsável.
     * Não permite cancelar agendamentos passados ou já finalizados/cancelados.
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public SchedulingResponseDTO cancel(UUID userId, UUID schedulingId) {
        Scheduling scheduling = repository.findById(schedulingId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        assertUserHasAccess(scheduling, userId);

        scheduling.cancel(clock.instant());

        return toDTO(scheduling);
    }

    /**
     * Remarcação atômica de agendamento:
     * <ul>
     *     <li>Valida permissão e se o agendamento não é passado ou finalizado;</li>
     *     <li>Adquire lock pessimista da agenda do barbeiro;</li>
     *     <li>Garante que o novo horário está dentro da jornada e sem bloqueios;</li>
     *     <li>Garante que nenhum outro agendamento ocupa o novo horário (ignora a si mesmo);</li>
     *     <li>Preserva o horário anterior para auditoria e incrementa o contador de remarcações;</li>
     *     <li>Se qualquer validação falhar, a transação reverte e o agendamento original fica intacto.</li>
     * </ul>
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public SchedulingResponseDTO reschedule(UUID userId, UUID schedulingId, dev.barbershop.api.scheduling.dto.SchedulingRescheduleDTO dto) {
        Scheduling scheduling = repository.findById(schedulingId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        assertUserHasAccess(scheduling, userId);

        Instant now = clock.instant();
        scheduling.validateCanModify(now);

        Instant newStart = dto.newStartAt().toInstant();
        validateBookingWindow(newStart);

        TimeInterval newPeriod = new TimeInterval(newStart, newStart.plus(Duration.ofMinutes(scheduling.getDurationTime())));
        UUID barberId = scheduling.getBarber().getId();

        guard.lockAgenda(barberId);

        guard.assertWithinWorkingHours(barberId, newPeriod);
        guard.assertSlotNotBlocked(barberId, newPeriod);
        guard.assertSlotFreeOfBookingsExcept(barberId, newPeriod, scheduling.getId());
        guard.assertClientFreeOfBookingsExcept(scheduling.getClient().getId(), newPeriod, scheduling.getId());

        scheduling.reschedule(newPeriod, now);

        guard.persistGuarded(() -> repository.saveAndFlush(scheduling));

        return toDTO(scheduling);
    }

    /**
     * Finaliza um atendimento realizado (muda para COMPLETED).
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public SchedulingResponseDTO complete(UUID userId, UUID schedulingId) {
        Scheduling scheduling = repository.findById(schedulingId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        assertUserHasAccess(scheduling, userId);

        scheduling.complete();

        return toDTO(scheduling);
    }

    /**
     * Retorna todos os agendamentos do dia do barbeiro logado.
     */
    @PreAuthorize("hasAnyRole('BARBER', 'BARBEIRO')")
    public java.util.List<BarberAppointmentHistoryDTO> findBarberDailySchedulings(UUID userId, LocalDate date) {
        Barber barber = guard.findBarberByUserId(userId);
        ZoneId zone = guard.zone();
        LocalDate targetDate = (date != null) ? date : LocalDate.now(clock.withZone(zone));

        Instant dayStart = targetDate.atStartOfDay(zone).toInstant();
        Instant dayEnd = targetDate.atTime(LocalTime.MAX).atZone(zone).toInstant();

        return repository.findDailyByBarber(barber.getId(), dayStart, dayEnd)
                .stream()
                .map(scheduling -> toHistoryDTO(scheduling, zone))
                .toList();
    }

    private void assertUserHasAccess(Scheduling scheduling, UUID userId) {
        boolean isOwnerClient = scheduling.getClient().getUser().getId().equals(userId);
        boolean isOwnerBarber = scheduling.getBarber().getUser().getId().equals(userId);

        if (!isOwnerClient && !isOwnerBarber) {
            throw new AccessDeniedException("Agendamento pertence a outro usuário");
        }
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

    @PreAuthorize("hasAnyRole('BARBER', 'BARBEIRO')")
    public Page<BarberAppointmentHistoryDTO> findBarberHistory(
            UUID userId,
            BarberSchedulingFilterDTO filters,
            Pageable pageable
    ) {
        Barber barber = guard.findBarberByUserId(userId);
        ZoneId zone = guard.zone();

        Specification<Scheduling> spec = SchedulingSpecification.forBarberWithFilters(barber.getId(), filters, zone);

        return repository.findAll(spec, pageable)
                .map(scheduling -> toHistoryDTO(scheduling, zone));
    }

    private BarberAppointmentHistoryDTO toHistoryDTO(Scheduling scheduling, ZoneId zone) {
        return new BarberAppointmentHistoryDTO(
                scheduling.getId(),
                scheduling.getClient().getId(),
                scheduling.getClient().getUser().getName(),
                scheduling.getService().getId(),
                scheduling.getService().getName(),
                scheduling.getStatus(),
                scheduling.getScheduledAt().atZone(zone).toOffsetDateTime(),
                scheduling.getEndsAt().atZone(zone).toOffsetDateTime(),
                scheduling.getDurationTime(),
                scheduling.getFinalPrice()
        );
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
                scheduling.getDurationTime(),
                scheduling.getStatus(),
                scheduling.getFinalPrice(),
                scheduling.getPreviousScheduledAt() != null ? scheduling.getPreviousScheduledAt().atZone(zone).toOffsetDateTime() : null,
                scheduling.getPreviousEndsAt() != null ? scheduling.getPreviousEndsAt().atZone(zone).toOffsetDateTime() : null,
                scheduling.getRescheduleCount()
        );
    }
}

