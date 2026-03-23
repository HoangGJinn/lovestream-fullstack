package com.hcmute.lovestream.service.notification;

import com.hcmute.lovestream.entity.Notification;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.UserNotificationStatus;
import com.hcmute.lovestream.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<Notification> getVisibleNotifications(String userId) {
        return notificationRepository.findByUser_IdAndStatusNotOrderBySentAtDesc(userId, UserNotificationStatus.DELETED);
    }

    @Transactional(readOnly = true)
    public List<Notification> getVisibleNotificationsByFilter(String userId, String filter) {
        String normalizedFilter = filter == null ? "all" : filter.trim().toLowerCase(Locale.ROOT);

        return switch (normalizedFilter) {
            case "unread" -> notificationRepository
                    .findByUser_IdAndStatusOrderBySentAtDesc(userId, UserNotificationStatus.UNREAD);
            case "read" -> notificationRepository
                    .findByUser_IdAndStatusOrderBySentAtDesc(userId, UserNotificationStatus.READ);
            default -> getVisibleNotifications(userId);
        };
    }

    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return notificationRepository.countByUser_IdAndStatus(userId, UserNotificationStatus.UNREAD);
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .ifPresent(notification -> {
                    if (notification.getStatus() == UserNotificationStatus.UNREAD) {
                        notification.setStatus(UserNotificationStatus.READ);
                    }
                });
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> notifications = notificationRepository
                .findByUser_IdAndStatusNotOrderBySentAtDesc(user.getId(), UserNotificationStatus.DELETED);

        for (Notification notification : notifications) {
            if (notification.getStatus() == UserNotificationStatus.UNREAD) {
                notification.setStatus(UserNotificationStatus.READ);
            }
        }
    }
}

