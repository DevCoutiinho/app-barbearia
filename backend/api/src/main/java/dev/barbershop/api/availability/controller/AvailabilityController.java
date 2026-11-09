package dev.barbershop.api.availability.controller;

import dev.barbershop.api.availability.controller.documentation.AvailabilityControllerDoc;
import dev.barbershop.api.availability.dto.AvailableSlotDTO;
import dev.barbershop.api.availability.service.AvailabilityService;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AvailabilityController implements AvailabilityControllerDoc {

    private final AvailabilityService service;

    @Override
    public ResponseEntity<ApiResponseDTO<AvailableSlotDTO>> findAvailableSlots(
            @PathVariable UUID barberId,
            @RequestParam UUID serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<AvailableSlotDTO> slots = service.findAvailableSlots(barberId, serviceId, date);

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Horários disponíveis retornados com sucesso",
                slots
        ));
    }
}
