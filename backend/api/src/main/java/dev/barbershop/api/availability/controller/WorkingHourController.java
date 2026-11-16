package dev.barbershop.api.availability.controller;

import dev.barbershop.api.availability.controller.documentation.WorkingHourControllerDoc;
import dev.barbershop.api.availability.dto.WeeklyScheduleDTO;
import dev.barbershop.api.availability.service.WorkingHourService;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WorkingHourController implements WorkingHourControllerDoc {

    private final WorkingHourService service;

    @Override
    public ResponseEntity<ApiResponseDTO<WeeklyScheduleDTO>> findMine(@AuthenticationPrincipal CustomUserDetails customUser) {
        WeeklyScheduleDTO schedule = service.findMine(customUser.getId());

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Grade semanal retornada com sucesso",
                List.of(schedule)
        ));
    }

    @Override
    public ResponseEntity<ApiResponseDTO<WeeklyScheduleDTO>> create(
            @RequestBody @Valid WeeklyScheduleDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        WeeklyScheduleDTO schedule = service.create(customUser.getId(), dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(
                HttpStatus.CREATED.value(),
                "Grade semanal criada com sucesso",
                List.of(schedule)
        ));
    }

    @Override
    public ResponseEntity<ApiResponseDTO<WeeklyScheduleDTO>> replaceMine(
            @RequestBody @Valid WeeklyScheduleDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        WeeklyScheduleDTO schedule = service.replaceMine(customUser.getId(), dto);

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Grade semanal atualizada com sucesso",
                List.of(schedule)
        ));
    }
}

