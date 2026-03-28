package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.entity.Notification;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.service.notification.NotificationService;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;
    private final UserProfileService userProfileService;

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
        notificationService.markAsRead(id, currentUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("id", notification.getId());
        response.put("type", notification.getType().name());
        response.put("title", notification.getTitle());
        response.put("content", notification.getContent());
        response.put("sentAt", notification.getSentAt());
        response.put("status", notification.getStatus().name());

        return ResponseEntity.ok(response);
    }
}

