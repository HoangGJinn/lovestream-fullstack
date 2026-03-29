package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.entity.Notification;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.service.notification.NotificationService;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private static final Set<String> ALLOWED_FILTERS = Set.of("all", "unread", "read");

    private final NotificationService notificationService;
    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<?> getNotifications(@RequestParam(name = "filter", defaultValue = "all") String filter,
                                              @RequestParam(name = "page", defaultValue = "0") int page,
                                              @RequestParam(name = "size", defaultValue = "20") int size,
                                              @RequestParam(name = "includeUnreadCount", defaultValue = "false") boolean includeUnreadCount,
                                              Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        String normalizedFilter = normalizeFilter(filter);
        Slice<Notification> notificationsPage = notificationService
                .getVisibleNotificationsByFilter(currentUser.getId(), normalizedFilter, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("items", notificationsPage.getContent().stream().map(this::toListPayload).toList());
        if (includeUnreadCount) {
            response.put("unreadCount", notificationService.countUnread(currentUser.getId()));
        }
        response.put("selectedFilter", normalizedFilter);
        response.put("page", notificationsPage.getNumber());
        response.put("size", notificationsPage.getSize());
        response.put("hasNext", notificationsPage.hasNext());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNotificationDetail(@PathVariable("id") String id,
                                                    Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        Optional<Notification> notificationOpt = notificationService.getNotificationDetail(id, currentUser.getId());

        if (notificationOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Thông báo không được tìm thấy")
            );
        }

        Notification notification = notificationOpt.get();
        return ResponseEntity.ok(toDetailPayload(notification));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable("id") String id,
                                        Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        Optional<Notification> notificationOpt = notificationService.markAsRead(id, currentUser.getId());
        if (notificationOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Thông báo không được tìm thấy"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("item", toListPayload(notificationOpt.get()));
        response.put("unreadCount", notificationService.countUnread(currentUser.getId()));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        long updatedCount = notificationService.markAllAsRead(currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("updatedCount", updatedCount);
        response.put("unreadCount", notificationService.countUnread(currentUser.getId()));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable("id") String id,
                                                Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        boolean deleted = notificationService.deleteNotification(id, currentUser.getId());
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Thông báo không được tìm thấy"));
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/open")
    public ResponseEntity<?> openNotification(@PathVariable("id") String id,
                                              Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        Optional<String> targetUrlOpt = notificationService.openNotification(id, currentUser.getId());
        if (targetUrlOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Thông báo không được tìm thấy"));
        }

        return ResponseEntity.ok(Map.of("redirectUrl", targetUrlOpt.get()));
    }

    private String normalizeFilter(String filter) {
        String normalized = filter == null ? "all" : filter.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_FILTERS.contains(normalized) ? normalized : "all";
    }

    private Map<String, Object> toListPayload(Notification notification) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", notification.getId());
        response.put("type", notification.getType().name());
        response.put("title", notification.getTitle());
        response.put("contentPreview", toPreview(notification.getContent()));
        response.put("sentAt", notification.getSentAt());
        response.put("status", notification.getStatus().name());
        response.put("targetUrl", notification.getTargetUrl());
        return response;
    }

    private Map<String, Object> toDetailPayload(Notification notification) {
        Map<String, Object> response = toListPayload(notification);
        response.put("content", notification.getContent());
        return response;
    }

    private String toPreview(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= 180) {
            return content;
        }
        return content.substring(0, 180) + "...";
    }
}

