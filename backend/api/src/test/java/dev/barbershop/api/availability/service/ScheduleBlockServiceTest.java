package dev.barbershop.api.availability.service;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.availability.dto.ScheduleBlockCreateDTO;
import dev.barbershop.api.availability.dto.ScheduleBlockResponseDTO;
import dev.barbershop.api.availability.entity.ScheduleBlock;
import dev.barbershop.api.availability.enums.BlockReason;
import dev.barbershop.api.availability.repository.ScheduleBlockRepository;
import dev.barbershop.api.barber.entity.Barber;
import dev.barbershop.api.common.exception.ResourceConflictException;
import dev.barbershop.api.config.scheduling.SchedulingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleBlockService - Regras de criação de bloqueio e conflitos")
class ScheduleBlockServiceTest {

    @Mock
    private AgendaGuard guard;

    @Mock
    private ScheduleBlockRepository repository;

    private ScheduleBlockService service;
    private final ZoneId zone = ZoneId.of("America/Sao_Paulo");
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), zone);

    @BeforeEach
    void setUp() {
        when(guard.zone()).thenReturn(zone);
        service = new ScheduleBlockService(guard, repository, fixedClock);
    }

    @Test
    @DisplayName("Regra 4: Deve recusar bloqueio se houver agendamento no horário com a mensagem exata")
    void shouldRejectBlockWhenBookingExists() {
        UUID userId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        Barber barber = Barber.builder().id(barberId).build();

        ScheduleBlockCreateDTO dto = new ScheduleBlockCreateDTO(
                LocalDate.of(2026, 11, 20),
                LocalTime.of(14, 30),
                LocalTime.of(16, 0),
                BlockReason.LUNCH,
                "Almoço"
        );

        when(guard.findBarberByUserId(userId)).thenReturn(barber);
        when(guard.lockAgenda(barberId)).thenReturn(barber);
        doThrow(new ResourceConflictException("Existe um agendamento neste horário. Remarque ou cancele o atendimento antes de bloquear."))
                .when(guard).assertNoBookingForBlock(eq(barberId), any(TimeInterval.class));

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.create(userId, dto));

        assertEquals("Existe um agendamento neste horário. Remarque ou cancele o atendimento antes de bloquear.", ex.getMessage());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Regra 8: Deve recusar bloqueio se houver outro bloqueio sobreposto")
    void shouldRejectBlockWhenOverlappingBlockExists() {
        UUID userId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        Barber barber = Barber.builder().id(barberId).build();

        ScheduleBlockCreateDTO dto = new ScheduleBlockCreateDTO(
                LocalDate.of(2026, 11, 20),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                BlockReason.LEAVE,
                "Saída"
        );

        when(guard.findBarberByUserId(userId)).thenReturn(barber);
        when(guard.lockAgenda(barberId)).thenReturn(barber);
        doNothing().when(guard).assertNoBookingForBlock(eq(barberId), any(TimeInterval.class));
        doThrow(new ResourceConflictException(AgendaGuard.BLOCK_CONFLICT))
                .when(guard).assertNoOverlappingBlock(eq(barberId), any(TimeInterval.class));

        assertThrows(ResourceConflictException.class, () -> service.create(userId, dto));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve permitir criar bloqueio com sucesso quando horário estiver livre")
    void shouldCreateBlockSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        Barber barber = Barber.builder().id(barberId).build();

        ScheduleBlockCreateDTO dto = new ScheduleBlockCreateDTO(
                LocalDate.of(2026, 11, 20),
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                BlockReason.LUNCH,
                "Almoço pontual"
        );

        when(guard.findBarberByUserId(userId)).thenReturn(barber);
        when(guard.lockAgenda(barberId)).thenReturn(barber);
        when(guard.persistGuarded(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleBlockResponseDTO response = service.create(userId, dto);

        assertNotNull(response);
        assertEquals(BlockReason.LUNCH, response.reason());
        assertEquals("Almoço pontual", response.description());
        verify(guard).lockAgenda(barberId);
        verify(guard).assertNoBookingForBlock(eq(barberId), any());
        verify(guard).assertNoOverlappingBlock(eq(barberId), any());
    }

    @Test
    @DisplayName("Deve rejeitar bloqueio em horário passado")
    void shouldRejectBlockInThePast() {
        UUID userId = UUID.randomUUID();
        ScheduleBlockCreateDTO dto = new ScheduleBlockCreateDTO(
                LocalDate.of(2026, 11, 19), // Ontem em relação ao clock
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                BlockReason.DAY_OFF,
                "Folga passada"
        );

        assertThrows(IllegalArgumentException.class, () -> service.create(userId, dto));
        verify(guard, never()).lockAgenda(any());
    }
}

