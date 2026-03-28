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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class NotificationWebController {

    private static final Set<String> ALLOWED_FILTERS = Set.of("all", "unread", "read");

    private final NotificationService notificationService;
    private final UserProfileService userProfileService;

    @GetMapping("/notifications")
    public String notificationPage(@RequestParam(name = "filter", defaultValue = "all") String filter,
                                   Authentication authentication,
                                   Model model) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());

        String normalizedFilter = normalizeFilter(filter);
        model.addAttribute("notifications", notificationService.getVisibleNotificationsByFilter(currentUser.getId(), normalizedFilter));
        model.addAttribute("unreadCount", notificationService.countUnread(currentUser.getId()));
        model.addAttribute("selectedFilter", normalizedFilter);
        return "user/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationAsRead(@PathVariable("id") String id,
                                         @RequestParam(name = "filter", defaultValue = "all") String filter,
                                         Authentication authentication) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        notificationService.markAsRead(id, currentUser.getId());
        return "redirect:/notifications?filter=" + normalizeFilter(filter);
    }

    @GetMapping("/notifications/{id}/open")
    public String openNotification(@PathVariable("id") String id,
                                   Authentication authentication) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        return "redirect:" + notificationService.openNotification(id, currentUser.getId());
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(@RequestParam(name = "filter", defaultValue = "all") String filter,
                                Authentication authentication) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        notificationService.markAllAsRead(currentUser);
        return "redirect:/notifications?filter=" + normalizeFilter(filter);
    }

    @GetMapping("/notifications/{id}/detail")
    public String viewNotificationDetail(@PathVariable("id") String id,
                                         Authentication authentication,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        Optional<Notification> notificationOpt = notificationService.getNotificationDetail(id, currentUser.getId());

        if (notificationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông báo không được tìm thấy.");
            return "redirect:/notifications";
        }

        Notification notification = notificationOpt.get();
        notificationService.markAsRead(id, currentUser.getId());

        model.addAttribute("notification", notification);
        model.addAttribute("currentUser", currentUser);
        return "user/notification-detail";
    }

    @PostMapping("/notifications/{id}/delete")
    public String deleteNotification(@PathVariable("id") String id,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        notificationService.deleteNotification(id, currentUser.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Thông báo đã được xóa.");
        return "redirect:/notifications";
    }

    private String normalizeFilter(String filter) {
        String normalized = filter == null ? "all" : filter.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_FILTERS.contains(normalized) ? normalized : "all";
    }
}

