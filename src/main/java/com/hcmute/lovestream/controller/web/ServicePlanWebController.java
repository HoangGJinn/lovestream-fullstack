package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.response.ServicePlanResponse;
import com.hcmute.lovestream.entity.Subscription;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import com.hcmute.lovestream.repository.SubscriptionRepository;
import com.hcmute.lovestream.service.plan.ServicePlanService;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ServicePlanWebController {

    private final ServicePlanService servicePlanService;
    private final UserProfileService userProfileService;
    private final SubscriptionRepository subscriptionRepository;

    // GET /packages — Danh sách tất cả gói dịch vụ đang active
    @GetMapping("/packages")
    public String getAllPackages(Authentication authentication, Model model) {
        log.info("GET /packages");
        List<ServicePlanResponse> plans = servicePlanService.getAllActivePlans();
        model.addAttribute("plans", plans);
        addCurrentSubscriptionModel(authentication, model);

        return "plans/list";
    }

    // GET /packages/{id} — Chi tiết 1 gói
    @GetMapping("/packages/{id}")
    public String getPackageDetail(@PathVariable String id, Authentication authentication, Model model) {
        log.info("GET /packages/{}", id);
        try {
            ServicePlanResponse plan = servicePlanService.getPlanById(id);
            model.addAttribute("plan", plan);
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }

        addCurrentSubscriptionModel(authentication, model);

        ServicePlanResponse selectedPlan = (ServicePlanResponse) model.getAttribute("plan");
        Subscription activeSubscription = (Subscription) model.getAttribute("activeSubscription");
        if (selectedPlan != null && activeSubscription != null && activeSubscription.getPlan() != null) {
            BigDecimal difference = selectedPlan.getPrice().subtract(activeSubscription.getPlan().getPrice());
            model.addAttribute("upgradeDifference", difference);
            model.addAttribute("isUpgradeTarget", difference.compareTo(BigDecimal.ZERO) > 0);
            model.addAttribute("isDowngradeTarget", difference.compareTo(BigDecimal.ZERO) < 0);
        }

        return "plans/detail";
    }

    private void addCurrentSubscriptionModel(Authentication authentication, Model model) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            model.addAttribute("currentPlanId", null);
            model.addAttribute("activeSubscription", null);
            return;
        }

        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());

        Subscription activeSubscription = subscriptionRepository
                .findTopByUserAndStatusOrderByEndDateDesc(currentUser, SubscriptionStatus.ACTIVE)
                .orElse(null);
        model.addAttribute("activeSubscription", activeSubscription);
        model.addAttribute("currentPlanId", activeSubscription != null ? activeSubscription.getPlan().getId() : null);
        model.addAttribute("currentPlanPrice", activeSubscription != null ? activeSubscription.getPlan().getPrice() : null);
    }
}
