package dev.barbershop.api.availability.service;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.availability.dto.ScheduleBlockCreateDTO;
import dev.barbershop.api.availability.dto.ScheduleBlockResponseDTO;
import dev.barbershop.api.availability.entity.ScheduleBlock;
import dev.barbershop.api.availability.repository.ScheduleBlockRepository;
import dev.barbershop.api.barber.entity.Barber;
import dev.barbershop.api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Bloqueios pontuais da agenda (regras 2 e 4).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleBlockService {

    private static final int DEFAULT_LIST_DAYS = 30;
    private static final int MAX_LIST_DAYS = 93;

    private final AgendaGuard guard;
    private final ScheduleBlockRepository repository;
    private final Clock clock;

    @PreAuthorize("hasRole('BARBER')")
    public List<ScheduleBlockResponseDTO> findMine(UUID userId, LocalDate from, LocalDate to) {
        ZoneId zone = guard.zone();
        LocalDate start = from != null ? from : LocalDate.now(clock.withZone(zone));
        LocalDate end = to != null ? to : start.plusDays(DEFAULT_LIST_DAYS);

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("A data final deve ser igual ou posterior à data inicial");
        }
        if (start.plusDays(MAX_LIST_DAYS).isBefore(end)) {
            throw new IllegalArgumentException("O período consultado não pode ultrapassar " + MAX_LIST_DAYS + " dias");
        }

        Barber barber = guard.findBarberByUserId(userId);

        return repository.findAllInPeriod(
                        barber.getId(),
                        start.atStartOfDay(zone).toInstant(),
                        end.plusDays(1).atStartOfDay(zone).toInstant()
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Fluxo: validações sem I/O -> lock da agenda -> conflito com agendamentos -> conflito com
     * bloqueios -> gravação. A ordem importa: as checagens só são confiáveis depois do lock.
     */
    @PreAuthorize("hasRole('BARBER')")
    @Transactional
    public ScheduleBlockResponseDTO create(UUID userId, ScheduleBlockCreateDTO dto) {
        TimeInterval period = toPeriod(dto);

        if (!period.end().isAfter(clock.instant())) {
            throw new IllegalArgumentException("Não é possível bloquear um período que já passou");
        }

        Barber barber = guard.lockAgenda(guard.findBarberByUserId(userId).getId());

        guard.assertNoBookingForBlock(barber.getId(), period);
        guard.assertNoOverlappingBlock(barber.getId(), period);

        ScheduleBlock block = guard.persistGuarded(() -> repository.saveAndFlush(
                ScheduleBlock.of(barber, period, dto.reason(), normalize(dto.description()))
        ));

        return toDTO(block);
    }

    /**
     * Remover um bloqueio apenas libera tempo na agenda, nunca gera conflito; por isso não precisa do lock.
     */
    @PreAuthorize("hasRole('BARBER')")
    @Transactional
    public void delete(UUID userId, UUID blockId) {
        Barber barber = guard.findBarberByUserId(userId);

        ScheduleBlock block = repository.findByIdAndBarberId(blockId, barber.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueio não encontrado para este barbeiro"));

        repository.delete(block);
    }

    /** Converte data + horários locais (fuso da barbearia) em período absoluto. Sem horários = dia inteiro. */
    private TimeInterval toPeriod(ScheduleBlockCreateDTO dto) {
        ZoneId zone = guard.zone();

        if (dto.startTime() == null && dto.endTime() == null) {
            return new TimeInterval(
                    dto.date().atStartOfDay(zone).toInstant(),
                    dto.date().plusDays(1).atStartOfDay(zone).toInstant()
            );
        }

        if (dto.startTime() == null || dto.endTime() == null) {
            throw new IllegalArgumentException(
                    "Informe o horário inicial e o final, ou omita ambos para bloquear o dia inteiro");
        }

        return new TimeInterval(
                dto.date().atTime(dto.startTime()).atZone(zone).toInstant(),
                dto.date().atTime(dto.endTime()).atZone(zone).toInstant()
        );
    }

    private String normalize(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private ScheduleBlockResponseDTO toDTO(ScheduleBlock block) {
        ZoneId zone = guard.zone();
        return new ScheduleBlockResponseDTO(
                block.getId(),
                block.getStartsAt().atZone(zone).toOffsetDateTime(),
                block.getEndsAt().atZone(zone).toOffsetDateTime(),
                block.getReason(),
                block.getReason().getLabel(),
                block.getDescription()
        );
    }
}

