package dev.barbershop.api.notification.controller.documentation;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.notification.dto.NotificationResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Notificações", description = "Consulta das notificações do usuário")
@RequestMapping("/notifications")
public interface NotificationControllerDoc {

    @Operation(
            summary = "Consultar minhas notificações",
            description = "Retorna as notificações do usuário autenticado, das mais recentes para as mais antigas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificações retornadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    @GetMapping("/me")
    ResponseEntity<ApiResponseDTO<NotificationResponseDTO>> findMyNotifications(
            @AuthenticationPrincipal CustomUserDetails customUser
    );
}
