package dev.barbershop.api.scheduling.controller;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.scheduling.controller.documentation.SchedulingControllerDoc;
import dev.barbershop.api.scheduling.dto.BarberAppointmentHistoryDTO;
import dev.barbershop.api.scheduling.dto.BarberSchedulingFilterDTO;
import dev.barbershop.api.scheduling.dto.SchedulingCreateDTO;
import dev.barbershop.api.scheduling.dto.SchedulingResponseDTO;
import dev.barbershop.api.scheduling.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/schedulings")
@RequiredArgsConstructor
public class SchedulingController implements SchedulingControllerDoc {

    private final SchedulingService service;

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<SchedulingResponseDTO>> findMySchedulings(
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        List<SchedulingResponseDTO> schedulings = service.findMySchedulings(customUser.getId());

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Agendamentos retornados com sucesso",
                schedulings
        ));
    }

    @Override
    @GetMapping({"/barber/history", "/history"})
    public ResponseEntity<ApiResponseDTO<Page<BarberAppointmentHistoryDTO>>> findBarberHistory(
            BarberSchedulingFilterDTO filters,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        Page<BarberAppointmentHistoryDTO> history = service.findBarberHistory(customUser.getId(), filters, pageable);

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Histórico de atendimentos retornado com sucesso",
                List.of(history)
        ));
    }

    @Override
    public ResponseEntity<ApiResponseDTO<SchedulingResponseDTO>> create(
            @RequestBody @Valid SchedulingCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        SchedulingResponseDTO scheduling = service.create(customUser.getId(), dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(
                HttpStatus.CREATED.value(),
                "Agendamento criado com sucesso",
                List.of(scheduling)
        ));
    }

    @Override
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponseDTO<SchedulingResponseDTO>> complete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        SchedulingResponseDTO scheduling = service.complete(customUser.getId(), id);

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Atendimento finalizado com sucesso",
                List.of(scheduling)
        ));
    }

    @Override
    @GetMapping({"/barber/daily", "/barber/today"})
    public ResponseEntity<ApiResponseDTO<BarberAppointmentHistoryDTO>> findBarberDaily(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        List<BarberAppointmentHistoryDTO> dailySchedulings = service.findBarberDailySchedulings(customUser.getId(), date);

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Agendamentos do dia retornados com sucesso",
                dailySchedulings
        ));
    }

    @Override
    public ResponseEntity<ApiResponseDTO<SchedulingResponseDTO>> reschedule(
            @PathVariable UUID id,
            @RequestBody @Valid dev.barbershop.api.scheduling.dto.SchedulingRescheduleDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        SchedulingResponseDTO scheduling = service.reschedule(customUser.getId(), id, dto);

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Agendamento remarcado com sucesso",
                List.of(scheduling)
        ));
    }

    @Override
    public ResponseEntity<ApiResponseDTO<SchedulingResponseDTO>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        SchedulingResponseDTO scheduling = service.cancel(customUser.getId(), id);

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Agendamento cancelado com sucesso",
                List.of(scheduling)
        ));
    }
}

