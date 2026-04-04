package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.entity.Notification;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.service.notification.NotificationService;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;
    private final UserProfileService userProfileService;

    // ===== 1. GET LIST =====
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "all") String filter,
            Authentication auth
    ) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userProfileService.getCurrentUserByEmail(auth.getName());

        List<Notification> list = notificationService
                .getNotifications(user.getId(), filter);

        Map<String, Object> res = new HashMap<>();
        res.put("items", list.stream().map(this::toListPayload).toList());
        res.put("unreadCount", notificationService.countUnread(user.getId()));

        return ResponseEntity.ok(res);
    }

    // ===== 2. DETAIL =====
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable String id, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userProfileService.getCurrentUserByEmail(auth.getName());

        Optional<Notification> opt = notificationService
                .getNotificationDetail(id, user.getId());

        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not found"));
        }

        return ResponseEntity.ok(toDetailPayload(opt.get()));
    }

    // ===== 3. MARK READ =====
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable String id, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userProfileService.getCurrentUserByEmail(auth.getName());
        Optional<Notification> opt = notificationService.getNotificationDetail(id, user.getId());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not found"));
        }

        notificationService.markAsRead(id, user.getId());

        return ResponseEntity.ok(Map.of(
                "unreadCount", notificationService.countUnread(user.getId())
        ));
    }

    // ===== 4. DELETE =====
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userProfileService.getCurrentUserByEmail(auth.getName());
        Optional<Notification> opt = notificationService.getNotificationDetail(id, user.getId());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not found"));
        }

        notificationService.delete(id, user.getId());

        return ResponseEntity.noContent().build();
    }

    // ===== MAPPER =====
    private Map<String, Object> toListPayload(Notification n) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", n.getId());
        res.put("title", n.getTitle());
        res.put("type", n.getType().name());
        res.put("contentPreview", toPreview(n.getContent()));
        res.put("sentAt", n.getSentAt());
        res.put("status", n.getStatus().name());
        res.put("targetUrl", n.getTargetUrl());
        return res;
    }

    private Map<String, Object> toDetailPayload(Notification n) {
        Map<String, Object> res = toListPayload(n);
        res.put("content", n.getContent());
        return res;
    }

    private String toPreview(String content) {
        if (content == null) return "";
        return content.length() <= 180 ? content : content.substring(0, 180) + "...";
    }
}