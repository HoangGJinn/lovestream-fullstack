package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.service.notification.NotificationService;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserProfileService userProfileService;
    private final NotificationService notificationService;

    @ModelAttribute
    public void addGlobalAttributes(Authentication authentication, Model model) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("unreadCount", 0L);

        if (isAuthenticated) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof Map<?, ?> principalMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentUser = (Map<String, Object>) principalMap;

                model.addAttribute("currentUser", currentUser);
                model.addAttribute("hasActiveSub", Boolean.TRUE.equals(currentUser.get("isVip")));

                Object emailValue = currentUser.get("email");
                if (emailValue instanceof String email && !email.isBlank()) {
                    try {
                        User user = userProfileService.getCurrentUserByEmail(email);
                        model.addAttribute("unreadCount", notificationService.countUnread(user.getId()));
                    } catch (RuntimeException ignored) {
                        model.addAttribute("unreadCount", 0L);
                    }
                }
            }
        } else {
            model.addAttribute("hasActiveSub", false);
        }
    }
}
