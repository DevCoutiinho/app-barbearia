package dev.barbershop.api.auth.authorization.controller;

import dev.barbershop.api.auth.authorization.controller.documentation.PermissionControllerDoc;
import dev.barbershop.api.auth.authorization.dto.PermissionCreateDTO;
import dev.barbershop.api.auth.authorization.dto.PermissionSummaryDTO;
import dev.barbershop.api.auth.authorization.service.PermissionService;
import dev.barbershop.api.common.dto.ApiResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PermissionController implements PermissionControllerDoc {

    private final PermissionService service;

    @Override
    public ResponseEntity<ApiResponseDTO<PermissionSummaryDTO>> findAll() {

        List<PermissionSummaryDTO> permissions = service.findAllSummaries();

        ApiResponseDTO<PermissionSummaryDTO> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Permissões retornadas com sucesso",
                permissions
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<PermissionSummaryDTO>> findById(UUID id) {
        PermissionSummaryDTO permission = service.findById(id);

        ApiResponseDTO<PermissionSummaryDTO> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Permissão retornada com sucesso",
                List.of(permission)
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> create(@Valid @RequestBody PermissionCreateDTO dto) {

        service.create(dto);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Permissão criada com sucesso"
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable UUID id) {
        service.delete(id);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Permissão excluída com sucesso"
        );

        return ResponseEntity.ok(apiResponseDTO);
    }
}
