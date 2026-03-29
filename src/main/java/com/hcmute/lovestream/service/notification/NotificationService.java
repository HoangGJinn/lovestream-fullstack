package com.hcmute.lovestream.service.notification;

import com.hcmute.lovestream.entity.Notification;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.TypeNotification;
import com.hcmute.lovestream.entity.enums.UserNotificationStatus;
import com.hcmute.lovestream.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

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

    @Transactional(readOnly = true)
    public Optional<Notification> getNotificationDetail(String notificationId, String userId) {
        return notificationRepository.findByIdAndUser_Id(notificationId, userId);
    }

    @Transactional
    public void deleteNotification(String notificationId, String userId) {
        notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .ifPresent(notification -> {
                    notification.setStatus(UserNotificationStatus.DELETED);
                    notificationRepository.save(notification);
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

    @Transactional
    public Notification createNotification(User user,
                                           TypeNotification type,
                                           String title,
                                           String content,
                                           String targetUrl,
                                           String dedupeKey) {
        if (user == null) {
            throw new IllegalArgumentException("User khong duoc de trong khi tao notification");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type notification khong duoc de trong");
        }
        if (!hasText(title)) {
            throw new IllegalArgumentException("Title notification khong duoc de trong");
        }
        if (!hasText(content)) {
            throw new IllegalArgumentException("Content notification khong duoc de trong");
        }

        String normalizedDedupeKey = normalizeNullable(dedupeKey);
        if (normalizedDedupeKey != null
                && notificationRepository.existsByUser_IdAndDedupeKey(user.getId(), normalizedDedupeKey)) {
            return null;
        }

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title.trim())
                .content(content.trim())
                .targetUrl(normalizeNullable(targetUrl))
                .dedupeKey(normalizedDedupeKey)
                .status(UserNotificationStatus.UNREAD)
                .build();

        return notificationRepository.save(notification);
    }


    @Transactional
    public String openNotification(String notificationId, String userId) {
        return notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .map(notification -> {
                    if (notification.getStatus() == UserNotificationStatus.UNREAD) {
                        notification.setStatus(UserNotificationStatus.READ);
                    }
                    return hasText(notification.getTargetUrl()) ? notification.getTargetUrl() : "/notifications";
                })
                .orElse("/notifications");
    }



    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }



    private String normalizeNullable(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
