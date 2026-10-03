package dev.barbershop.api.user.admin.controller;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.user.admin.controller.documentation.UserControllerDoc;
import dev.barbershop.api.user.admin.dto.UserAdminResponseDTO;
import dev.barbershop.api.user.admin.dto.UserFilterDTO;
import dev.barbershop.api.user.admin.dto.UserUpdateRolesDTO;
import dev.barbershop.api.user.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerDoc {

    private final UserService service;

    @Override
    public ResponseEntity<ApiResponseDTO<Page<UserAdminResponseDTO>>> findAll(UserFilterDTO dto, Pageable pageable) {

        Page<UserAdminResponseDTO> users = service.findAll(dto, pageable);

        ApiResponseDTO<Page<UserAdminResponseDTO>> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Usuários retornados com sucesso",
                List.of(users)
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> updateRoles(UUID id, UserUpdateRolesDTO dto) {

        service.updateRoles(id, dto);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Roles do usuário alteradas com sucesso",
                List.of()
        );

        return ResponseEntity.ok(apiResponseDTO);
    }

    @Override
    public ResponseEntity<ApiResponseDTO<Void>> delete(UUID id, @AuthenticationPrincipal CustomUserDetails customUser) {

        service.delete(id, customUser.getId());

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Usuário deletado com sucesso",
                List.of()
        );

        return ResponseEntity.ok(apiResponseDTO);
    }


}