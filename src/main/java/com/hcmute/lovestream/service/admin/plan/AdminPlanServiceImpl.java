package com.hcmute.lovestream.service.admin.plan;

import com.hcmute.lovestream.dto.request.admin.plan.UpdatePlanRequest;
import com.hcmute.lovestream.entity.ServicePlan;
import com.hcmute.lovestream.repository.ServicePlanRepository;
import com.hcmute.lovestream.service.admin.plan.AdminPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPlanServiceImpl implements AdminPlanService {

    private final ServicePlanRepository servicePlanRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllPlans() {
        List<ServicePlan> plans = servicePlanRepository.findAll();

        // Map dữ liệu để trả về cho Frontend
        return plans.stream().map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "name", p.getName(),
                "price", p.getPrice(),

                // ĐÃ SỬA: Lấy số ngày từ DB
                "durationDays", p.getDurationDays(),

                // ĐÃ SỬA: Dùng hàm isActive() của kiểu boolean
                "status", p.isActive() ? "ACTIVE" : "INACTIVE"
        )).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updatePlan(String id, UpdatePlanRequest request) {
        ServicePlan plan = servicePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói dịch vụ"));

        // Cập nhật thông tin (Main Flow - Bước 7)
        plan.setName(request.getName().trim());
        plan.setPrice(request.getPrice());

        servicePlanRepository.save(plan);
    }
}