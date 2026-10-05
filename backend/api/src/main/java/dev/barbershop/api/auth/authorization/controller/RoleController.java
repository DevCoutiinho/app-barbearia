package dev.barbershop.api.auth.authorization.controller;

import dev.barbershop.api.auth.authorization.controller.documentation.RoleControllerDoc;
import dev.barbershop.api.auth.authorization.dto.RoleCreateDTO;
import dev.barbershop.api.auth.authorization.dto.RoleResponseDTO;
import dev.barbershop.api.auth.authorization.dto.RoleUpdateDTO;
import dev.barbershop.api.auth.authorization.service.RoleService;
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
public class RoleController implements RoleControllerDoc {
    private final RoleService service;

    @Override
    public ResponseEntity<ApiResponseDTO<RoleResponseDTO>> findAll() {
        List<RoleResponseDTO> roles = service.findAll();

        ApiResponseDTO<RoleResponseDTO> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Roles retornadas com sucesso",
                roles
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> create(@Valid @RequestBody RoleCreateDTO dto) {

        service.create(dto);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Role criada com sucesso"
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> update(@Valid @RequestBody RoleUpdateDTO dto, @PathVariable UUID id) {
        service.update(id, dto);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Role atualizada com sucesso"
        );

        return ResponseEntity.ok(apiResponseDTO);
    }


    @Override
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable UUID id) {
        service.delete(id);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Role excluída com sucesso"
        );

        return ResponseEntity.ok(apiResponseDTO);
    }
}
