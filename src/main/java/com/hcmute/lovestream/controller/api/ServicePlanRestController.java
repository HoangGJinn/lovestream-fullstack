package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.response.PurchaseResponse;
import com.hcmute.lovestream.dto.response.ServicePlanResponse;
import com.hcmute.lovestream.service.plan.ServicePlanService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class ServicePlanRestController {

    private final ServicePlanService servicePlanService;

    // GET /api/v1/plans — Danh sách gói đang hoạt động
    @GetMapping
    public ResponseEntity<List<ServicePlanResponse>> getAllActivePlans() {
        return ResponseEntity.ok(servicePlanService.getAllActivePlans());
    }

    // POST /api/v1/plans/{id}/purchase — Mua gói dịch vụ (yêu cầu đăng nhập)
    @PostMapping("/{id}/purchase")
    public ResponseEntity<?> purchasePackage(@PathVariable String id,
                                             Authentication authentication,
                                             HttpServletRequest request) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Vui lòng đăng nhập để mua gói dịch vụ"));
        }

        try {
            PurchaseResponse response = servicePlanService.purchasePlan(authentication.getName(), id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
