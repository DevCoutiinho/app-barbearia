package dev.barbershop.api.availability.controller;

import dev.barbershop.api.availability.controller.documentation.ScheduleBlockControllerDoc;
import dev.barbershop.api.availability.dto.ScheduleBlockCreateDTO;
import dev.barbershop.api.availability.dto.ScheduleBlockResponseDTO;
import dev.barbershop.api.availability.service.ScheduleBlockService;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ScheduleBlockController implements ScheduleBlockControllerDoc {

    private final ScheduleBlockService service;

    @Override
    public ResponseEntity<ApiResponseDTO<ScheduleBlockResponseDTO>> findMine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        List<ScheduleBlockResponseDTO> blocks = service.findMine(customUser.getId(), from, to);

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Bloqueios retornados com sucesso",
                blocks
        ));
    }

    @Override
    public ResponseEntity<ApiResponseDTO<ScheduleBlockResponseDTO>> create(
            @RequestBody @Valid ScheduleBlockCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        ScheduleBlockResponseDTO block = service.create(customUser.getId(), dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(
                HttpStatus.CREATED.value(),
                "Bloqueio criado com sucesso",
                List.of(block)
        ));
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        service.delete(customUser.getId(), id);

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Bloqueio removido com sucesso"
        ));
    }
}

