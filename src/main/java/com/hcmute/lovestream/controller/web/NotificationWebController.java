package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.service.notification.NotificationService;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import com.hcmute.lovestream.entity.Notification;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class NotificationWebController {

    private static final Set<String> ALLOWED_FILTERS = Set.of("all", "unread", "read");
    private static final int INITIAL_PAGE_SIZE = 20;

    private final NotificationService notificationService;
    private final UserProfileService userProfileService;

    @GetMapping("/notifications")
    public String notificationPage(@RequestParam(name = "filter", defaultValue = "all") String filter,
                                   Authentication authentication,
                                   Model model) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());

        String normalizedFilter = normalizeFilter(filter);
        Slice<Notification> firstPage = notificationService
                .getVisibleNotificationsByFilter(currentUser.getId(), normalizedFilter, 0, INITIAL_PAGE_SIZE);

        model.addAttribute("notifications", firstPage.getContent());
        model.addAttribute("initialPage", 0);
        model.addAttribute("pageSize", INITIAL_PAGE_SIZE);
        model.addAttribute("hasNextPage", firstPage.hasNext());
        model.addAttribute("unreadCount", notificationService.countUnread(currentUser.getId()));
        model.addAttribute("selectedFilter", normalizedFilter);
        return "user/notifications";
    }

    private String normalizeFilter(String filter) {
        String normalized = filter == null ? "all" : filter.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_FILTERS.contains(normalized) ? normalized : "all";
    }



}

