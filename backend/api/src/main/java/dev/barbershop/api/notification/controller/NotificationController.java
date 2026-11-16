package dev.barbershop.api.notification.controller;

import dev.barbershop.api.common.dto.ApiResponseDTO;
import dev.barbershop.api.config.security.CustomUserDetails;
import dev.barbershop.api.notification.controller.documentation.NotificationControllerDoc;
import dev.barbershop.api.notification.dto.NotificationResponseDTO;
import dev.barbershop.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDoc {

    private final NotificationService service;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<NotificationResponseDTO>> findMyNotifications(
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        List<NotificationResponseDTO> notifications = service.findUserNotifications(customUser.getId())
                .stream()
                .map(NotificationResponseDTO::from)
                .toList();

        return ResponseEntity.ok(new ApiResponseDTO<>(
                HttpStatus.OK.value(),
                "Notificações retornadas com sucesso",
                notifications
        ));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails customUser
    ) {
        service.markAsRead(customUser.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
