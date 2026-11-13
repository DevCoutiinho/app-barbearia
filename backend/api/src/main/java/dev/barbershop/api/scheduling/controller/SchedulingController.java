package dev.barbershop.api.scheduling.controller;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.scheduling.dto.SchedulingRequestDTO;
import dev.barbershop.api.scheduling.dto.SchedulingResponseDTO;
import dev.barbershop.api.scheduling.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedulings")
public class SchedulingController {

    private final SchedulingService service;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<Void>> create(
            @AuthenticationPrincipal CustomUserDetails customUser,
            @Valid @RequestBody SchedulingRequestDTO dto
    ) {
        service.create(
                customUser.getId(),
                dto.barberId(),
                dto.serviceId(),
                dto.scheduledAt()
        );

        ApiResponseDTO<Void> response = new ApiResponseDTO<>(
                HttpStatus.CREATED.value(),
                "Agendamento criado com sucesso"
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<SchedulingResponseDTO>> findMySchedulings(
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        List<SchedulingResponseDTO> schedulings =
                service.findMySchedulings(customUser.getId());

        ApiResponseDTO<SchedulingResponseDTO> response =
                new ApiResponseDTO<>(
                        HttpStatus.OK.value(),
                        "Agendamentos encontrados com sucesso",
                        schedulings
                );

        return ResponseEntity.ok(response);
    }
}
