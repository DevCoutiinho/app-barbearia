package dev.barbershop.api.notification.service;

import dev.barbershop.api.notification.entity.Notification;
import dev.barbershop.api.notification.enums.NotificationType;
import dev.barbershop.api.notification.repository.NotificationRepository;
import dev.barbershop.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification notify(User recipient, String title, String message, NotificationType type) {
        Objects.requireNonNull(recipient, "O destinatário da notificação é obrigatório");
        Objects.requireNonNull(title, "O título da notificação é obrigatório");
        Objects.requireNonNull(message, "A mensagem da notificação é obrigatória");
        Objects.requireNonNull(type, "O tipo da notificação é obrigatório");

        Notification notification = Notification.builder()
                .user(recipient)
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .build();

        log.info("Notificação criada para o usuário [{}]: título='{}', tipo='{}'", recipient.getId(), title, type);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> findUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Page<Notification> findUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUser().getId().equals(userId)) {
                notification.markAsRead();
                notificationRepository.save(notification);
            }
        });
    }
}
