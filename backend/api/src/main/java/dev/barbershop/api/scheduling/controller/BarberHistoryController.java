package dev.barbershop.api.scheduling.controller;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.scheduling.dto.BarberAppointmentHistoryDTO;
import dev.barbershop.api.scheduling.dto.BarberSchedulingFilterDTO;
import dev.barbershop.api.scheduling.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/barbers/me")
@RequiredArgsConstructor
public class BarberHistoryController {

    private final SchedulingService service;

    @PreAuthorize("hasAnyRole('BARBER', 'BARBEIRO')")
    @GetMapping({"/history", "/history/"})
    public ResponseEntity<ApiResponseDTO<Page<BarberAppointmentHistoryDTO>>> findHistory(
            @ParameterObject @ModelAttribute BarberSchedulingFilterDTO filters,
            @ParameterObject @PageableDefault(size = 10, sort = "scheduledAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        Page<BarberAppointmentHistoryDTO> history = service.findBarberHistory(customUser.getId(), filters, pageable);

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Histórico de atendimentos retornado com sucesso",
                List.of(history)
        ));
    }

    @PreAuthorize("hasAnyRole('BARBER', 'BARBEIRO')")
    @GetMapping({"/daily", "/daily/"})
    public ResponseEntity<ApiResponseDTO<BarberAppointmentHistoryDTO>> findDaily(
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
}

