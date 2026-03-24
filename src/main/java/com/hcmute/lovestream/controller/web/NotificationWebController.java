package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.service.notification.NotificationService;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class NotificationWebController {

    private final NotificationService notificationService;
    private final UserProfileService userProfileService;

    @GetMapping("/notifications")
    public String notificationPage(@RequestParam(name = "filter", defaultValue = "all") String filter,
                                   Authentication authentication,
                                   Model model) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());


        model.addAttribute("notifications", notificationService.getVisibleNotificationsByFilter(currentUser.getId(), filter));
        model.addAttribute("unreadCount", notificationService.countUnread(currentUser.getId()));
        model.addAttribute("selectedFilter", filter);
        return "user/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationAsRead(@PathVariable("id") String id,
                                         @RequestParam(name = "filter", defaultValue = "all") String filter,
                                         Authentication authentication) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        notificationService.markAsRead(id, currentUser.getId());
        return "redirect:/notifications?filter=" + filter;
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(@RequestParam(name = "filter", defaultValue = "all") String filter,
                                Authentication authentication) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        notificationService.markAllAsRead(currentUser);
        return "redirect:/notifications?filter=" + filter;
    }
}

