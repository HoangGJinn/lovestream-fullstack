package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.response.PurchaseResponse;
import com.hcmute.lovestream.service.plan.ServicePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class ServicePlanRestController {

    private final ServicePlanService servicePlanService;

    // POST /api/v1/plans/{id}/purchase — Mua gói dịch vụ (yêu cầu đăng nhập)
    @PostMapping("/{id}/purchase")
    public ResponseEntity<?> purchasePackage(@PathVariable String id,
                                             Authentication authentication) {
        try {
            PurchaseResponse response = servicePlanService.purchasePlan(authentication.getName(), id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
