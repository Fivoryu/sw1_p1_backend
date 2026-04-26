package com.banco.workflow.service;

import com.banco.workflow.model.Notification;
import com.banco.workflow.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getNotificationsByUserId(String userId, Integer limit) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(limit)
                .toList();
    }

    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(String notificationId) {
        Notification notif = notificationRepository.findById(notificationId).orElse(null);
        if (notif != null) {
            notif.setRead(true);
            notif.setReadAt(LocalDateTime.now());
            notificationRepository.save(notif);
        }
    }

    public void markAllAsRead(String userId) {
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }

    public Notification createNotification(String userId, String title, String body, String type, String relatedId) {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .title(title)
                .body(body)
                .type(type)
                .read(false)
                .createdAt(LocalDateTime.now())
                .relatedId(relatedId)
                .priority("MEDIUM")
                .build();
        return notificationRepository.save(notification);
    }
}
