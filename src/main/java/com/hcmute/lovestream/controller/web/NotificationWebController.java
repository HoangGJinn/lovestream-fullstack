package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.Notification;
import com.hcmute.lovestream.service.notification.NotificationService;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class NotificationWebController {

    private final NotificationService notificationService;
    private final UserProfileService userProfileService;

    @GetMapping("/notifications")
    public String notificationPage(
            @RequestParam(defaultValue = "all") String filter,
            Authentication auth,
            Model model
    ) {
        User user = userProfileService.getCurrentUserByEmail(auth.getName());

        List<Notification> list = notificationService
                .getNotifications(user.getId(), filter);

        String normalizedFilter = filter == null ? "all" : filter.trim().toLowerCase();

        model.addAttribute("notifications", list);
        model.addAttribute("unreadCount",
                notificationService.countUnread(user.getId()));
        model.addAttribute("selectedFilter", normalizedFilter);

        return "user/notifications";
    }



}

