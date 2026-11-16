package dev.barbershop.api.notification.dto;

import dev.barbershop.api.notification.entity.Notification;
import dev.barbershop.api.notification.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponseDTO(
        UUID id,
        String title,
        String message,
        NotificationType type,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponseDTO from(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                Boolean.TRUE.equals(notification.getRead()),
                notification.getCreatedAt()
        );
    }
}
