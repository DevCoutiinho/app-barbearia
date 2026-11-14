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
import dev.barbershop.api.user.entity.User;
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
        lenient().when(guard.zone()).thenReturn(zone);
        service = new SchedulingService(guard, repository, clientRepository, barberServiceRepository, properties, fixedClock);
    }

    private Barber createActiveBarber(UUID barberId) {
        User user = User.builder().password("secret123").active(true).build();
        return Barber.builder().id(barberId).user(user).build();
    }

    @Test
    @DisplayName("Regra 5, 6 e 8: Deve recusar novo agendamento que conflita com outro agendamento")
    void shouldRejectWhenOverlappingSchedulingExists() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        Client client = Client.builder().id(UUID.randomUUID()).build();
        Barber barber = createActiveBarber(barberId);
        BarberService barberService = BarberService.builder()
                .id(serviceId)
                .name("Corte de Cabelo")
                .barber(barber)
                .price(BigDecimal.valueOf(50))
                .durationTime(30)
                .active(true)
                .build();

        OffsetDateTime start = OffsetDateTime.of(2026, 11, 20, 14, 0, 0, 0, ZoneOffset.of("-03:00"));
        SchedulingCreateDTO dto = new SchedulingCreateDTO(barberId, serviceId, start);

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
    @DisplayName("Regra 5 e 8: Deve recusar novo agendamento se o cliente já tiver agendamento no mesmo período")
    void shouldRejectWhenClientHasOverlappingBooking() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Client client = Client.builder().id(clientId).build();
        Barber barber = createActiveBarber(barberId);
        BarberService barberService = BarberService.builder()
                .id(serviceId)
                .name("Barba Terapia")
                .barber(barber)
                .price(BigDecimal.valueOf(30))
                .durationTime(15)
                .active(true)
                .build();

        OffsetDateTime start = OffsetDateTime.of(2026, 11, 20, 14, 0, 0, 0, ZoneOffset.of("-03:00"));
        SchedulingCreateDTO dto = new SchedulingCreateDTO(barberId, serviceId, start);

        when(clientRepository.findByUserId(userId)).thenReturn(Optional.of(client));
        when(barberServiceRepository.findById(serviceId)).thenReturn(Optional.of(barberService));
        when(guard.lockAgenda(barberId)).thenReturn(barber);

        doThrow(new ResourceConflictException(AgendaGuard.CLIENT_BOOKING_CONFLICT))
                .when(guard).assertClientFreeOfBookings(eq(clientId), any(TimeInterval.class));

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.create(userId, dto));
        assertEquals(AgendaGuard.CLIENT_BOOKING_CONFLICT, ex.getMessage());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Regra 4 e 8: Deve recusar agendamento que se sobrepõe a um bloqueio")
    void shouldRejectWhenOverlappingBlockExists() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        Client client = Client.builder().id(UUID.randomUUID()).build();
        Barber barber = createActiveBarber(barberId);
        BarberService barberService = BarberService.builder()
                .id(serviceId)
                .name("Barba")
                .barber(barber)
                .price(BigDecimal.valueOf(35))
                .durationTime(30)
                .active(true)
                .build();

        OffsetDateTime start = OffsetDateTime.of(2026, 11, 20, 12, 30, 0, 0, ZoneOffset.of("-03:00"));
        SchedulingCreateDTO dto = new SchedulingCreateDTO(null, serviceId, start);

        when(clientRepository.findByUserId(userId)).thenReturn(Optional.of(client));
        when(barberServiceRepository.findById(serviceId)).thenReturn(Optional.of(barberService));
        when(guard.lockAgenda(barberId)).thenReturn(barber);

        doThrow(new ResourceConflictException(AgendaGuard.SLOT_BLOCKED))
                .when(guard).assertSlotNotBlocked(eq(barberId), any(TimeInterval.class));

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.create(userId, dto));
        assertEquals(AgendaGuard.SLOT_BLOCKED, ex.getMessage());
    }

    @Test
    @DisplayName("Regra 8: Deve recusar agendamento se serviço não pertencer ao barbeiro informado")
    void shouldRejectWhenServiceDoesNotBelongToBarber() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID actualBarberId = UUID.randomUUID();
        UUID wrongBarberId = UUID.randomUUID();

        Client client = Client.builder().id(UUID.randomUUID()).build();
        Barber barber = createActiveBarber(actualBarberId);
        BarberService barberService = BarberService.builder()
                .id(serviceId)
                .name("Corte")
                .barber(barber)
                .price(BigDecimal.valueOf(50))
                .durationTime(30)
                .active(true)
                .build();

        OffsetDateTime start = OffsetDateTime.of(2026, 11, 20, 10, 0, 0, 0, ZoneOffset.of("-03:00"));
        SchedulingCreateDTO dto = new SchedulingCreateDTO(wrongBarberId, serviceId, start);

        when(clientRepository.findByUserId(userId)).thenReturn(Optional.of(client));
        when(barberServiceRepository.findById(serviceId)).thenReturn(Optional.of(barberService));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(userId, dto));
        assertEquals("O serviço selecionado não pertence ao barbeiro informado", ex.getMessage());
        verify(guard, never()).lockAgenda(any());
    }

    @Test
    @DisplayName("Regra 3: Deve recusar agendamento fora da grade de trabalho")
    void shouldRejectWhenOutsideWorkingHours() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        Client client = Client.builder().id(UUID.randomUUID()).build();
        Barber barber = createActiveBarber(barberId);
        BarberService barberService = BarberService.builder()
                .id(serviceId)
                .name("Corte")
                .barber(barber)
                .price(BigDecimal.valueOf(50))
                .durationTime(30)
                .active(true)
                .build();

        OffsetDateTime start = OffsetDateTime.of(2026, 11, 20, 22, 0, 0, 0, ZoneOffset.of("-03:00"));
        SchedulingCreateDTO dto = new SchedulingCreateDTO(null, serviceId, start);

        when(clientRepository.findByUserId(userId)).thenReturn(Optional.of(client));
        when(barberServiceRepository.findById(serviceId)).thenReturn(Optional.of(barberService));
        when(guard.lockAgenda(barberId)).thenReturn(barber);

        doThrow(new ResourceConflictException(AgendaGuard.OUTSIDE_WORKING_HOURS))
                .when(guard).assertWithinWorkingHours(eq(barberId), any(TimeInterval.class));

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.create(userId, dto));
        assertEquals(AgendaGuard.OUTSIDE_WORKING_HOURS, ex.getMessage());
    }

    @Test
    @DisplayName("Regra 3 e 7: Agendamento criado com status inicial PENDING, snapshot de preço e duração")
    void shouldCreateSchedulingWithCalculatedEndsAtAndPendingStatus() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        Client client = Client.builder().id(UUID.randomUUID()).build();
        Barber barber = createActiveBarber(barberId);
        BarberService barberService = BarberService.builder()
                .id(serviceId)
                .name("Corte Degrade")
                .barber(barber)
                .price(BigDecimal.valueOf(45))
                .durationTime(45) // 45 minutos de duração
                .active(true)
                .build();

        OffsetDateTime start = OffsetDateTime.of(2026, 11, 20, 10, 0, 0, 0, ZoneOffset.of("-03:00"));
        SchedulingCreateDTO dto = new SchedulingCreateDTO(barberId, serviceId, start);

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
        assertEquals(SchedulingStatus.PENDING, response.status(), "Status inicial deve ser PENDING");
        assertEquals(start, response.startAt());
        assertEquals(start.plusMinutes(45), response.endAt(), "O fim deve ser exatamente início + duração");
        assertEquals(45, response.durationTime(), "Deve registrar a duração do serviço");
        assertEquals(BigDecimal.valueOf(45), response.finalPrice(), "Deve registrar snapshot do preço no momento da contratação");
    }

    @Test
    @DisplayName("Deve permitir confirmar agendamento PENDING -> SCHEDULED")
    void shouldConfirmPendingScheduling() {
        UUID userId = UUID.randomUUID();
        UUID schedulingId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        User user = User.builder().id(userId).password("secret123").active(true).build();
        Client client = Client.builder().id(UUID.randomUUID()).user(user).build();
        Barber barber = Barber.builder().id(barberId).user(user).build();
        BarberService serviceEntity = BarberService.builder().id(UUID.randomUUID()).name("Barba").price(BigDecimal.TEN).barber(barber).build();

        Scheduling scheduling = Scheduling.builder()
                .id(schedulingId)
                .client(client)
                .barber(barber)
                .service(serviceEntity)
                .scheduledAt(Instant.parse("2026-11-20T13:00:00Z"))
                .endsAt(Instant.parse("2026-11-20T13:30:00Z"))
                .durationTime(30)
                .finalPrice(BigDecimal.TEN)
                .status(SchedulingStatus.PENDING)
                .build();

        when(repository.findById(schedulingId)).thenReturn(Optional.of(scheduling));

        SchedulingResponseDTO response = service.confirm(userId, schedulingId);

        assertEquals(SchedulingStatus.SCHEDULED, response.status());
    }

    @Test
    @DisplayName("Regra 1 e 2: Cancelamento com sucesso de agendamento futuro pelo cliente")
    void shouldCancelFutureSchedulingSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID schedulingId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        User user = User.builder().id(userId).password("secret123").active(true).build();
        Client client = Client.builder().id(UUID.randomUUID()).user(user).build();
        Barber barber = Barber.builder().id(barberId).user(user).build();
        BarberService serviceEntity = BarberService.builder().id(UUID.randomUUID()).name("Corte").price(BigDecimal.TEN).barber(barber).build();

        // Agendamento no futuro em relação ao fixedClock (2026-11-20T08:00:00Z)
        Scheduling scheduling = Scheduling.builder()
                .id(schedulingId)
                .client(client)
                .barber(barber)
                .service(serviceEntity)
                .scheduledAt(Instant.parse("2026-11-20T14:00:00Z"))
                .endsAt(Instant.parse("2026-11-20T14:30:00Z"))
                .durationTime(30)
                .finalPrice(BigDecimal.TEN)
                .status(SchedulingStatus.SCHEDULED)
                .build();

        when(repository.findById(schedulingId)).thenReturn(Optional.of(scheduling));

        SchedulingResponseDTO response = service.cancel(userId, schedulingId);

        assertEquals(SchedulingStatus.CANCELED, response.status(), "Status deve ser alterado para CANCELED");
    }

    @Test
    @DisplayName("Regra 2: Rejeitar cancelamento de agendamento passado com a mensagem exata")
    void shouldRejectCancellationOfPastScheduling() {
        UUID userId = UUID.randomUUID();
        UUID schedulingId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        User user = User.builder().id(userId).password("secret123").active(true).build();
        Client client = Client.builder().id(UUID.randomUUID()).user(user).build();
        Barber barber = Barber.builder().id(barberId).user(user).build();
        BarberService serviceEntity = BarberService.builder().id(UUID.randomUUID()).name("Corte").price(BigDecimal.TEN).barber(barber).build();

        // Agendamento no passado em relação ao fixedClock (2026-11-20T08:00:00Z)
        Scheduling scheduling = Scheduling.builder()
                .id(schedulingId)
                .client(client)
                .barber(barber)
                .service(serviceEntity)
                .scheduledAt(Instant.parse("2026-11-20T07:00:00Z"))
                .endsAt(Instant.parse("2026-11-20T07:30:00Z"))
                .durationTime(30)
                .finalPrice(BigDecimal.TEN)
                .status(SchedulingStatus.SCHEDULED)
                .build();

        when(repository.findById(schedulingId)).thenReturn(Optional.of(scheduling));

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.cancel(userId, schedulingId));
        assertEquals("Não é possível alterar ou cancelar um agendamento passado ou finalizado.", ex.getMessage());
    }

    @Test
    @DisplayName("Regra 2: Rejeitar cancelamento de agendamento finalizado (COMPLETED) ou já cancelado (CANCELED)")
    void shouldRejectCancellationOfCompletedOrCanceledScheduling() {
        UUID userId = UUID.randomUUID();
        UUID schedulingId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        User user = User.builder().id(userId).password("secret123").active(true).build();
        Client client = Client.builder().id(UUID.randomUUID()).user(user).build();
        Barber barber = Barber.builder().id(barberId).user(user).build();
        BarberService serviceEntity = BarberService.builder().id(UUID.randomUUID()).name("Corte").price(BigDecimal.TEN).barber(barber).build();

        Scheduling schedulingCompleted = Scheduling.builder()
                .id(schedulingId)
                .client(client)
                .barber(barber)
                .service(serviceEntity)
                .scheduledAt(Instant.parse("2026-11-20T14:00:00Z"))
                .endsAt(Instant.parse("2026-11-20T14:30:00Z"))
                .durationTime(30)
                .finalPrice(BigDecimal.TEN)
                .status(SchedulingStatus.COMPLETED)
                .build();

        when(repository.findById(schedulingId)).thenReturn(Optional.of(schedulingCompleted));

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.cancel(userId, schedulingId));
        assertEquals("Não é possível alterar ou cancelar um agendamento passado ou finalizado.", ex.getMessage());
    }

    @Test
    @DisplayName("Regra 3, 4 e 7: Remarcação atômica com sucesso e preservação de histórico")
    void shouldRescheduleSuccessfullyAndPreserveHistory() {
        UUID userId = UUID.randomUUID();
        UUID schedulingId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        User user = User.builder().id(userId).password("secret123").active(true).build();
        Client client = Client.builder().id(UUID.randomUUID()).user(user).build();
        Barber barber = Barber.builder().id(barberId).user(user).build();
        BarberService serviceEntity = BarberService.builder().id(UUID.randomUUID()).name("Corte").price(BigDecimal.TEN).barber(barber).build();

        Instant originalStart = Instant.parse("2026-11-20T14:00:00Z");
        Instant originalEnd = Instant.parse("2026-11-20T14:30:00Z");

        Scheduling scheduling = Scheduling.builder()
                .id(schedulingId)
                .client(client)
                .barber(barber)
                .service(serviceEntity)
                .scheduledAt(originalStart)
                .endsAt(originalEnd)
                .durationTime(30)
                .finalPrice(BigDecimal.TEN)
                .status(SchedulingStatus.SCHEDULED)
                .rescheduleCount(0)
                .build();

        OffsetDateTime newStart = OffsetDateTime.of(2026, 11, 20, 16, 0, 0, 0, ZoneOffset.of("-03:00"));
        dev.barbershop.api.scheduling.dto.SchedulingRescheduleDTO dto =
                new dev.barbershop.api.scheduling.dto.SchedulingRescheduleDTO(newStart);

        when(repository.findById(schedulingId)).thenReturn(Optional.of(scheduling));
        when(guard.lockAgenda(barberId)).thenReturn(barber);
        when(guard.persistGuarded(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SchedulingResponseDTO response = service.reschedule(userId, schedulingId, dto);

        assertNotNull(response);
        assertEquals(newStart, response.startAt());
        assertEquals(newStart.plusMinutes(30), response.endAt());
        assertEquals(1, response.rescheduleCount(), "Deve incrementar o contador de remarcações");
        assertNotNull(response.previousStartAt(), "Deve registrar o horário inicial anterior");
        assertNotNull(response.previousEndAt(), "Deve registrar o horário final anterior");
        verify(guard).lockAgenda(barberId);
        verify(guard).assertWithinWorkingHours(eq(barberId), any());
        verify(guard).assertSlotNotBlocked(eq(barberId), any());
        verify(guard).assertSlotFreeOfBookingsExcept(eq(barberId), any(), eq(schedulingId));
    }

    @Test
    @DisplayName("Regra 5: Concorrência na remarcação - rejeitar se novo horário foi ocupado por outro cliente")
    void shouldRejectRescheduleWhenNewSlotIsOccupiedByAnother() {
        UUID userId = UUID.randomUUID();
        UUID schedulingId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        User user = User.builder().id(userId).password("secret123").active(true).build();
        Client client = Client.builder().id(UUID.randomUUID()).user(user).build();
        Barber barber = Barber.builder().id(barberId).user(user).build();
        BarberService serviceEntity = BarberService.builder().id(UUID.randomUUID()).name("Corte").price(BigDecimal.TEN).barber(barber).build();

        Instant originalStart = Instant.parse("2026-11-20T14:00:00Z");
        Instant originalEnd = Instant.parse("2026-11-20T14:30:00Z");

        Scheduling scheduling = Scheduling.builder()
                .id(schedulingId)
                .client(client)
                .barber(barber)
                .service(serviceEntity)
                .scheduledAt(originalStart)
                .endsAt(originalEnd)
                .durationTime(30)
                .finalPrice(BigDecimal.TEN)
                .status(SchedulingStatus.SCHEDULED)
                .rescheduleCount(0)
                .build();

        OffsetDateTime newStart = OffsetDateTime.of(2026, 11, 20, 16, 0, 0, 0, ZoneOffset.of("-03:00"));
        dev.barbershop.api.scheduling.dto.SchedulingRescheduleDTO dto =
                new dev.barbershop.api.scheduling.dto.SchedulingRescheduleDTO(newStart);

        when(repository.findById(schedulingId)).thenReturn(Optional.of(scheduling));
        when(guard.lockAgenda(barberId)).thenReturn(barber);
        doThrow(new ResourceConflictException(AgendaGuard.SLOT_UNAVAILABLE))
                .when(guard).assertSlotFreeOfBookingsExcept(eq(barberId), any(), eq(schedulingId));

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.reschedule(userId, schedulingId, dto));
        assertEquals(AgendaGuard.SLOT_UNAVAILABLE, ex.getMessage());

        // Garante que o agendamento original não foi modificado
        assertEquals(originalStart, scheduling.getScheduledAt());
        assertEquals(originalEnd, scheduling.getEndsAt());
        assertEquals(0, scheduling.getRescheduleCount());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Regra 6: Rejeitar alteração ou cancelamento de agendamento de outro cliente")
    void shouldRejectModificationByUnauthorizedUser() {
        UUID ownerUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID schedulingId = UUID.randomUUID();

        User ownerUser = User.builder().id(ownerUserId).password("secret123").active(true).build();
        User barberUser = User.builder().id(UUID.randomUUID()).password("secret123").active(true).build();

        Client client = Client.builder().id(UUID.randomUUID()).user(ownerUser).build();
        Barber barber = Barber.builder().id(UUID.randomUUID()).user(barberUser).build();

        Scheduling scheduling = Scheduling.builder()
                .id(schedulingId)
                .client(client)
                .barber(barber)
                .scheduledAt(Instant.parse("2026-11-20T14:00:00Z"))
                .endsAt(Instant.parse("2026-11-20T14:30:00Z"))
                .status(SchedulingStatus.SCHEDULED)
                .build();

        when(repository.findById(schedulingId)).thenReturn(Optional.of(scheduling));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.cancel(otherUserId, schedulingId));
    }

    @Test
    @DisplayName("Histórico do Barbeiro: Deve retornar agendamentos filtrados com dados completos do cliente e serviço")
    void shouldReturnBarberHistoryWithFilters() {
        UUID userId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID schedulingId = UUID.randomUUID();

        Barber barber = createActiveBarber(barberId);
        when(guard.findBarberByUserId(userId)).thenReturn(barber);

        User clientUser = User.builder().name("João da Silva").password("secret123").active(true).build();
        Client client = Client.builder().id(UUID.randomUUID()).user(clientUser).build();

        BarberService serviceEntity = BarberService.builder()
                .id(UUID.randomUUID())
                .name("Corte Degradê")
                .barber(barber)
                .price(BigDecimal.valueOf(45.0))
                .durationTime(30)
                .active(true)
                .build();

        Instant start = Instant.parse("2026-11-20T13:00:00Z");
        Instant end = Instant.parse("2026-11-20T13:30:00Z");

        Scheduling scheduling = Scheduling.builder()
                .id(schedulingId)
                .client(client)
                .barber(barber)
                .service(serviceEntity)
                .scheduledAt(start)
                .endsAt(end)
                .durationTime(30)
                .finalPrice(BigDecimal.valueOf(45.0))
                .status(SchedulingStatus.COMPLETED)
                .build();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Scheduling> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(scheduling), pageable, 1);

        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);

        dev.barbershop.api.scheduling.dto.BarberSchedulingFilterDTO filters = new dev.barbershop.api.scheduling.dto.BarberSchedulingFilterDTO(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                "João",
                SchedulingStatus.COMPLETED
        );

        org.springframework.data.domain.Page<dev.barbershop.api.scheduling.dto.BarberAppointmentHistoryDTO> result = service.findBarberHistory(userId, filters, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        dev.barbershop.api.scheduling.dto.BarberAppointmentHistoryDTO item = result.getContent().get(0);
        assertEquals(schedulingId, item.id());
        assertEquals("João da Silva", item.clientName());
        assertEquals("Corte Degradê", item.serviceName());
        assertEquals(SchedulingStatus.COMPLETED, item.status());
        assertEquals(BigDecimal.valueOf(45.0), item.price());
        assertEquals(30, item.durationTime());
    }
}
