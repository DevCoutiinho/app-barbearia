package dev.barbershop.api.notification.service;

import dev.barbershop.api.notification.dto.NotificationResponseDTO;
import dev.barbershop.api.notification.entity.Notification;
import dev.barbershop.api.notification.enums.NotificationType;
import dev.barbershop.api.notification.repository.NotificationRepository;
import dev.barbershop.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional
    public void notifyAppointmentCanceled(User recipient, String serviceName) {

        Notification notification = Notification.builder()
                .user(recipient)
                .title("Agendamento cancelado")
                .message("Seu agendamento do serviço " + serviceName
                        + " foi cancelado pelo barbeiro.")
                .type(NotificationType.APPOINTMENT_CANCELED)
                .read(false)
                .build();

        repository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> findMyNotifications(UUID userId) {
        return repository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();
    }
}
