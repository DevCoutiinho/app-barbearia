package dev.barbershop.api.barberservice.controller;

import dev.barbershop.api.barberservice.controller.documentation.BarberServiceControllerDoc;
import dev.barbershop.api.barberservice.dto.BarberServiceCreateDTO;
import dev.barbershop.api.barberservice.dto.BarberServiceFilterDTO;
import dev.barbershop.api.barberservice.dto.BarberServiceResponseDTO;
import dev.barbershop.api.barberservice.dto.BarberServiceUpdateDTO;
import dev.barbershop.api.barberservice.service.BarberServiceApplicationService;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BarberServiceController implements BarberServiceControllerDoc {
    private final BarberServiceApplicationService service;

    @Override
    public ResponseEntity<ApiResponseDTO<Page<BarberServiceResponseDTO>>> findAll(
            @ParameterObject
            @ModelAttribute BarberServiceFilterDTO dto,

            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = {"name", "technique"},
                    direction = Sort.Direction.ASC
            ) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        Page<BarberServiceResponseDTO> services = service.findAll(customUser.getId(), dto, pageable);

        ApiResponseDTO<Page<BarberServiceResponseDTO>> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Serviço criado com sucesso",
                List.of(services)
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> create(
            @RequestPart("data") @Valid BarberServiceCreateDTO dto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {

        service.create(customUser.getId(), image, dto);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Serviço criado com sucesso",
                null
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> update(
            @PathVariable UUID id,
            @RequestPart("data") @Valid BarberServiceUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        service.update(id, customUser.getId(), dto);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Serviço atualizado com sucesso",
                null
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails customUser) {

        service.delete(id, customUser.getId());

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Serviço deletado com sucesso",
                null
        );

        return ResponseEntity.ok(apiResponseDTO);
    }
}
