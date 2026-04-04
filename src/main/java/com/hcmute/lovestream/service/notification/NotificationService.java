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

    private static final List<String> ALLOWED_FILTERS = List.of("all", "unread", "read");

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(String userId, String filter) {
        List<UserNotificationStatus> statuses = switch (normalizeFilter(filter)) {
            case "unread" -> List.of(UserNotificationStatus.UNREAD);
            case "read" -> List.of(UserNotificationStatus.READ);
            default -> List.of(UserNotificationStatus.UNREAD, UserNotificationStatus.READ);
        };

        return notificationRepository
                .findByUser_IdAndStatusInOrderBySentAtDesc(userId, statuses);
    }

    @Transactional(readOnly = true)
    public Optional<Notification> getNotificationDetail(String id, String userId) {
        return getVisibleNotification(id, userId);
    }

    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return notificationRepository.countByUser_IdAndStatus(userId, UserNotificationStatus.UNREAD);
    }

    @Transactional
    public void markAsRead(String id, String userId) {
        getVisibleNotification(id, userId)
                .filter(notification -> notification.getStatus() == UserNotificationStatus.UNREAD)
                .ifPresent(n -> n.setStatus(UserNotificationStatus.READ));
    }

    @Transactional
    public void delete(String id, String userId) {
        getVisibleNotification(id, userId)
                .ifPresent(n -> n.setStatus(UserNotificationStatus.DELETED));
    }

    private String normalizeFilter(String filter) {
        String normalized = filter == null ? "all" : filter.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_FILTERS.contains(normalized) ? normalized : "all";
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

    private Optional<Notification> getVisibleNotification(String notificationId, String userId) {
        return notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .filter(notification -> notification.getStatus() != UserNotificationStatus.DELETED);
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
