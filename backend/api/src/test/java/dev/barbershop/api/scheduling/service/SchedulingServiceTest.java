package dev.barbershop.api.scheduling.service;

import dev.barbershop.api.availability.domain.TimeInterval;
import dev.barbershop.api.availability.service.AgendaGuard;
import dev.barbershop.api.barber.entity.Barber;
import dev.barbershop.api.barberservice.entity.BarberService;
import dev.barbershop.api.barberservice.repository.BarberServiceRepository;
import dev.barbershop.api.client.entity.Client;
import dev.barbershop.api.client.repository.ClientRepository;
import dev.barbershop.api.common.exception.ResourceConflictException;
import dev.barbershop.api.config.scheduling.SchedulingProperties;
import dev.barbershop.api.scheduling.dto.SchedulingCreateDTO;
import dev.barbershop.api.scheduling.dto.SchedulingResponseDTO;
import dev.barbershop.api.scheduling.entity.Scheduling;
import dev.barbershop.api.scheduling.enums.SchedulingStatus;
import dev.barbershop.api.scheduling.repository.SchedulingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulingService - Conflitos de agendamento, status e concorrência")
class SchedulingServiceTest {

    @Mock
    private AgendaGuard guard;

    @Mock
    private SchedulingRepository repository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private BarberServiceRepository barberServiceRepository;

    private SchedulingService service;
    private final ZoneId zone = ZoneId.of("America/Sao_Paulo");
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-20T08:00:00Z"), zone);
    private final SchedulingProperties properties = new SchedulingProperties(zone, 15, 60);

    @BeforeEach
    void setUp() {
        when(guard.zone()).thenReturn(zone);
        service = new SchedulingService(guard, repository, clientRepository, barberServiceRepository, properties, fixedClock);
    }

    @Test
    @DisplayName("Regra 5 e 8: Deve recusar novo agendamento que conflita com outro agendamento")
    void shouldRejectWhenOverlappingSchedulingExists() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        Client client = Client.builder().id(UUID.randomUUID()).build();
        Barber barber = Barber.builder().id(barberId).build();
        BarberService barberService = BarberService.builder()
                .id(serviceId)
                .name("Corte de Cabelo")
                .barber(barber)
                .price(BigDecimal.valueOf(50))
                .durationTime(30)
                .active(true)
                .build();

        OffsetDateTime start = OffsetDateTime.of(2026, 11, 20, 14, 0, 0, 0, ZoneOffset.of("-03:00"));
        SchedulingCreateDTO dto = new SchedulingCreateDTO(serviceId, start);

        when(clientRepository.findByUserId(userId)).thenReturn(Optional.of(client));
        when(barberServiceRepository.findById(serviceId)).thenReturn(Optional.of(barberService));
        when(guard.lockAgenda(barberId)).thenReturn(barber);

        doThrow(new ResourceConflictException(AgendaGuard.SLOT_UNAVAILABLE))
                .when(guard).assertSlotFreeOfBookings(eq(barberId), any(TimeInterval.class));

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.create(userId, dto));
        assertEquals(AgendaGuard.SLOT_UNAVAILABLE, ex.getMessage());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Regra 4 e 8: Deve recusar agendamento que se sobrepõe a um bloqueio")
    void shouldRejectWhenOverlappingBlockExists() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        Client client = Client.builder().id(UUID.randomUUID()).build();
        Barber barber = Barber.builder().id(barberId).build();
        BarberService barberService = BarberService.builder()
                .id(serviceId)
                .name("Barba")
                .barber(barber)
                .price(BigDecimal.valueOf(35))
                .durationTime(30)
                .active(true)
                .build();

        OffsetDateTime start = OffsetDateTime.of(2026, 11, 20, 12, 30, 0, 0, ZoneOffset.of("-03:00"));
        SchedulingCreateDTO dto = new SchedulingCreateDTO(serviceId, start);

        when(clientRepository.findByUserId(userId)).thenReturn(Optional.of(client));
        when(barberServiceRepository.findById(serviceId)).thenReturn(Optional.of(barberService));
        when(guard.lockAgenda(barberId)).thenReturn(barber);

        doThrow(new ResourceConflictException(AgendaGuard.SLOT_BLOCKED))
                .when(guard).assertSlotNotBlocked(eq(barberId), any(TimeInterval.class));

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.create(userId, dto));
        assertEquals(AgendaGuard.SLOT_BLOCKED, ex.getMessage());
    }

    @Test
    @DisplayName("Regra 3: Deve recusar agendamento fora da grade de trabalho")
    void shouldRejectWhenOutsideWorkingHours() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        Client client = Client.builder().id(UUID.randomUUID()).build();
        Barber barber = Barber.builder().id(barberId).build();
        BarberService barberService = BarberService.builder()
                .id(serviceId)
                .name("Corte")
                .barber(barber)
                .price(BigDecimal.valueOf(50))
                .durationTime(30)
                .active(true)
                .build();

        OffsetDateTime start = OffsetDateTime.of(2026, 11, 20, 22, 0, 0, 0, ZoneOffset.of("-03:00")); // Fora do expediente
        SchedulingCreateDTO dto = new SchedulingCreateDTO(serviceId, start);

        when(clientRepository.findByUserId(userId)).thenReturn(Optional.of(client));
        when(barberServiceRepository.findById(serviceId)).thenReturn(Optional.of(barberService));
        when(guard.lockAgenda(barberId)).thenReturn(barber);

        doThrow(new ResourceConflictException(AgendaGuard.OUTSIDE_WORKING_HOURS))
                .when(guard).assertWithinWorkingHours(eq(barberId), any(TimeInterval.class));

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.create(userId, dto));
        assertEquals(AgendaGuard.OUTSIDE_WORKING_HOURS, ex.getMessage());
    }

    @Test
    @DisplayName("Regra 6: Agendamento criado com sucesso calcula endsAt baseado na duração do serviço")
    void shouldCreateSchedulingWithCalculatedEndsAt() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        Client client = Client.builder().id(UUID.randomUUID()).build();
        Barber barber = Barber.builder().id(barberId).build();
        BarberService barberService = BarberService.builder()
                .id(serviceId)
                .name("Corte Degrade")
                .barber(barber)
                .price(BigDecimal.valueOf(45))
                .durationTime(45) // 45 minutos de duração
                .active(true)
                .build();

        OffsetDateTime start = OffsetDateTime.of(2026, 11, 20, 10, 0, 0, 0, ZoneOffset.of("-03:00"));
        SchedulingCreateDTO dto = new SchedulingCreateDTO(serviceId, start);

        when(clientRepository.findByUserId(userId)).thenReturn(Optional.of(client));
        when(barberServiceRepository.findById(serviceId)).thenReturn(Optional.of(barberService));
        when(guard.lockAgenda(barberId)).thenReturn(barber);
        when(guard.persistGuarded(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SchedulingResponseDTO response = service.create(userId, dto);

        assertNotNull(response);
        assertEquals(SchedulingStatus.SCHEDULED, response.status());
        assertEquals(start, response.startAt());
        assertEquals(start.plusMinutes(45), response.endAt(), "O fim deve ser exatamente início + duração");
        assertEquals(BigDecimal.valueOf(45), response.finalPrice());
    }
}

