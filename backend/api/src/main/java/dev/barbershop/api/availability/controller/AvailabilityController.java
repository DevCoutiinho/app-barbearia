package dev.barbershop.api.availability.controller;

import dev.barbershop.api.availability.dto.AvailableSlotDTO;
import dev.barbershop.api.availability.service.AvailabilityService;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/availability")
public class AvailabilityController {

    private final AvailabilityService service;

    @GetMapping("/barbers/{barberId}/slots")
    public ResponseEntity<ApiResponseDTO<AvailableSlotDTO>> findAvailableSlots(
            @PathVariable UUID barberId,
            @RequestParam UUID serviceId,
            @RequestParam LocalDate date
    ) {

        // Busca os horários disponíveis através do Service
        List<AvailableSlotDTO> slots = service.findAvailableSlots(
                barberId,
                serviceId,
                date
        );

        // Monta a resposta da API
        ApiResponseDTO<AvailableSlotDTO> response =
                new ApiResponseDTO<>(
                        HttpStatus.OK.value(),
                        "Horários disponíveis encontrados",
                        slots
                );

        return ResponseEntity.ok(response);
    }
}
