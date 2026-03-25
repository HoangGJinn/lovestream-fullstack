package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.admin.plan.UpdatePlanRequest;
import com.hcmute.lovestream.service.admin.plan.AdminPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/plans")
@RequiredArgsConstructor
public class AdminPlanRestController {

    private final AdminPlanService adminPlanService;

    // Lấy danh sách gói
    @GetMapping
    public ResponseEntity<?> getAllPlans() {
        return ResponseEntity.ok(adminPlanService.getAllPlans());
    }

    // Cập nhật gói
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable String id, @Valid @RequestBody UpdatePlanRequest request) {
        try {
            adminPlanService.updatePlan(id, request);
            return ResponseEntity.ok(Map.of("message", "Cập nhật thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}