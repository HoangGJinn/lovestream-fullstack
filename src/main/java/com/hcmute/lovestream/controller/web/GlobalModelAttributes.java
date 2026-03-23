package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.service.plan.ServicePlanService;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Controller
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserProfileService userProfileService;
    private final ServicePlanService servicePlanService;

    @ModelAttribute
    public void addGlobalAttributes(Authentication authentication, Model model) {
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            if (!model.containsAttribute("currentUser")) {
                try {
                    model.addAttribute("currentUser",
                            userProfileService.getCurrentUserByEmail(authentication.getName()));
                } catch (RuntimeException ignored) {
                }
            }

            if (!model.containsAttribute("hasActiveSub")) {
                boolean hasActiveSub = false;
                try {
                    hasActiveSub = servicePlanService.hasActiveSubscription(authentication.getName());
                } catch (RuntimeException ignored) {
                }
                model.addAttribute("hasActiveSub", hasActiveSub);
            }
        } else if (!model.containsAttribute("hasActiveSub")) {
            model.addAttribute("hasActiveSub", false);
        }
    }
}

