package dev.barbershop.api.availability.service;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.availability.entity.WorkingHour;
import dev.barbershop.api.availability.repository.ScheduleBlockRepository;
import dev.barbershop.api.availability.repository.WorkingHourRepository;
import dev.barbershop.api.barber.entity.Barber;
import dev.barbershop.api.barber.repository.BarberRepository;
import dev.barbershop.api.common.exception.ResourceConflictException;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import dev.barbershop.api.config.scheduling.SchedulingProperties;
import dev.barbershop.api.scheduling.enums.SchedulingStatus;
import dev.barbershop.api.scheduling.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Concentra as regras de consistência da agenda, reutilizadas por bloqueios, grade e agendamentos.
 * <p>
 * Estratégia de concorrência (em camadas):
 * <ol>
 *     <li><b>Lock da agenda do barbeiro</b> ({@link #lockAgenda}): SELECT ... FOR UPDATE na linha de
 *     {@code barbers}. Serializa, por barbeiro, todas as escritas que podem gerar conflito
 *     (Agendamento x Agendamento, Agendamento x Bloqueio, Bloqueio x Bloqueio). Como o lock é obtido
 *     ANTES das validações, a validação sempre enxerga os dados já confirmados pela transação anterior.</li>
 *     <li><b>Validações</b> (assert*): retornam mensagens de negócio claras.</li>
 *     <li><b>EXCLUDE constraints no banco</b>: rede de segurança para Agendamento x Agendamento e
 *     Bloqueio x Bloqueio, mesmo que alguma escrita não passe por este fluxo.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class AgendaGuard {

    public static final String BOOKING_CONFLICT_ON_BLOCK =
            "Existe um agendamento neste horário. Remarque ou cancele o atendimento antes de bloquear.";
    public static final String BLOCK_CONFLICT =
            "Já existe um bloqueio que se sobrepõe a este período.";
    public static final String SLOT_UNAVAILABLE =
            "Este horário acabou de ser reservado por outra pessoa. Por favor, escolha outro.";
    public static final String SLOT_BLOCKED =
            "O barbeiro bloqueou este horário. Escolha outro horário.";
    public static final String CLIENT_BOOKING_CONFLICT =
            "Você já possui um agendamento ativo neste mesmo período.";
    public static final String OUTSIDE_WORKING_HOURS =
            "O horário solicitado está fora do horário de trabalho do barbeiro.";

    private static final String SCHEDULING_OVERLAP_CONSTRAINT = "excl_scheduling_overlap";
    private static final String BLOCK_OVERLAP_CONSTRAINT = "excl_schedule_block_overlap";

    private final BarberRepository barberRepository;
    private final WorkingHourRepository workingHourRepository;
    private final ScheduleBlockRepository blockRepository;
    private final SchedulingRepository schedulingRepository;
    private final SchedulingProperties properties;

    public ZoneId zone() {
        return properties.zoneId();
    }

    public Barber findBarberByUserId(UUID userId) {
        return barberRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Não existe barbeiro associado ao usuário informado"));
    }

    /**
     * Adquire o lock exclusivo da agenda do barbeiro até o fim da transação corrente.
     * Exige transação ativa: fora de uma transação o lock seria liberado imediatamente.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Barber lockAgenda(UUID barberId) {
        return barberRepository.findByIdForUpdate(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado"));
    }

    /** Agendamento x Bloqueio (regra 4): só agendamentos que ocupam a agenda contam. */
    public void assertNoBookingForBlock(UUID barberId, TimeInterval period) {
        if (hasActiveBooking(barberId, period)) {
            throw new ResourceConflictException(BOOKING_CONFLICT_ON_BLOCK);
        }
    }

    /** Bloqueio x Bloqueio. */
    public void assertNoOverlappingBlock(UUID barberId, TimeInterval period) {
        if (blockRepository.existsOverlapping(barberId, period.start(), period.end())) {
            throw new ResourceConflictException(BLOCK_CONFLICT);
        }
    }

    /** Agendamento x Agendamento (regra 5). */
    public void assertSlotFreeOfBookings(UUID barberId, TimeInterval period) {
        if (hasActiveBooking(barberId, period)) {
            throw new ResourceConflictException(SLOT_UNAVAILABLE);
        }
    }

    /** Agendamento x Agendamento ignorando um ID de agendamento (durante remarcação). */
    public void assertSlotFreeOfBookingsExcept(UUID barberId, TimeInterval period, UUID excludedSchedulingId) {
        boolean hasOverlap = schedulingRepository.existsOverlappingExcept(
                barberId,
                period.start(),
                period.end(),
                SchedulingStatus.occupyingStatuses(),
                excludedSchedulingId
        );
        if (hasOverlap) {
            throw new ResourceConflictException(SLOT_UNAVAILABLE);
        }
    }

    /** Garante que o cliente não tem outro agendamento simultâneo (mesmo com outro barbeiro). */
    public void assertClientFreeOfBookings(UUID clientId, TimeInterval period) {
        boolean hasOverlap = schedulingRepository.existsOverlappingForClient(
                clientId,
                period.start(),
                period.end(),
                SchedulingStatus.occupyingStatuses()
        );
        if (hasOverlap) {
            throw new ResourceConflictException(CLIENT_BOOKING_CONFLICT);
        }
    }

    /** Validação de agenda do cliente ignorando um agendamento específico (durante remarcação). */
    public void assertClientFreeOfBookingsExcept(UUID clientId, TimeInterval period, UUID excludedSchedulingId) {
        boolean hasOverlap = schedulingRepository.existsOverlappingForClientExcept(
                clientId,
                period.start(),
                period.end(),
                SchedulingStatus.occupyingStatuses(),
                excludedSchedulingId
        );
        if (hasOverlap) {
            throw new ResourceConflictException(CLIENT_BOOKING_CONFLICT);
        }
    }

    /** Agendamento x Bloqueio, do ponto de vista do cliente. */
    public void assertSlotNotBlocked(UUID barberId, TimeInterval period) {
        if (blockRepository.existsOverlapping(barberId, period.start(), period.end())) {
            throw new ResourceConflictException(SLOT_BLOCKED);
        }
    }

    /** O atendimento inteiro precisa caber em UM intervalo da grade vigente (regra 3). */
    public void assertWithinWorkingHours(UUID barberId, TimeInterval period) {
        LocalDate date = period.start().atZone(zone()).toLocalDate();

        boolean fits = workIntervalsOn(barberId, date).stream()
                .anyMatch(work -> work.contains(period));

        if (!fits) {
            throw new ResourceConflictException(OUTSIDE_WORKING_HOURS);
        }
    }

    /** Converte a grade (LocalTime) do dia da semana da data em instantes absolutos no fuso da barbearia. */
    public List<TimeInterval> workIntervalsOn(UUID barberId, LocalDate date) {
        List<WorkingHour> workingHours =
                workingHourRepository.findByBarberIdAndDayOfWeekOrderByStartTimeAsc(barberId, date.getDayOfWeek());

        return workingHours.stream()
                .map(wh -> new TimeInterval(
                        date.atTime(wh.getStartTime()).atZone(zone()).toInstant(),
                        date.atTime(wh.getEndTime()).atZone(zone()).toInstant()
                ))
                .toList();
    }

    /**
     * Executa a gravação traduzindo violações das EXCLUDE constraints em mensagens de negócio.
     * Só deveria ocorrer se alguma escrita tiver ignorado o lock da agenda.
     */
    public <T> T persistGuarded(Supplier<T> persistence) {
        try {
            return persistence.get();
        } catch (DataIntegrityViolationException e) {
            String cause = String.valueOf(NestedExceptionUtils.getMostSpecificCause(e).getMessage());

            if (cause.contains(SCHEDULING_OVERLAP_CONSTRAINT)) {
                throw new ResourceConflictException(SLOT_UNAVAILABLE);
            }
            if (cause.contains(BLOCK_OVERLAP_CONSTRAINT)) {
                throw new ResourceConflictException(BLOCK_CONFLICT);
            }
            throw e;
        }
    }

    private boolean hasActiveBooking(UUID barberId, TimeInterval period) {
        return schedulingRepository.existsOverlapping(
                barberId,
                period.start(),
                period.end(),
                SchedulingStatus.occupyingStatuses()
        );
    }
}

