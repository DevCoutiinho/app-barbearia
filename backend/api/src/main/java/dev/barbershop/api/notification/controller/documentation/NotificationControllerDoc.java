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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notificações", description = "Visualização e gerenciamento de notificações do usuário")
@RequestMapping("/notifications")
public interface NotificationControllerDoc {

    @Operation(summary = "Listar minhas notificações", description = "Retorna o histórico de notificações do usuário logado ordenadas da mais recente para a mais antiga.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificações retornadas com sucesso")
    })
    @GetMapping("/me")
    ResponseEntity<ApiResponseDTO<NotificationResponseDTO>> findMyNotifications(
            @AuthenticationPrincipal CustomUserDetails customUser
    );

    @Operation(summary = "Marcar notificação como lida", description = "Marca uma notificação do usuário como lida.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notificação marcada como lida com sucesso")
    })
    @PatchMapping("/{id}/read")
    ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails customUser
    );
}
