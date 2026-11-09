package dev.barbershop.api.scheduling.controller;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.scheduling.controller.documentation.SchedulingControllerDoc;
import dev.barbershop.api.scheduling.dto.SchedulingCreateDTO;
import dev.barbershop.api.scheduling.dto.SchedulingResponseDTO;
import dev.barbershop.api.scheduling.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SchedulingController implements SchedulingControllerDoc {

    private final SchedulingService service;

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

