package com.hcmute.lovestream.service.admin.plan;

import com.hcmute.lovestream.dto.request.admin.plan.UpdatePlanRequest;

import java.util.List;
import java.util.Map;

public interface AdminPlanService {

    // Main Flow - Bước 2: Lấy danh sách tất cả các gói dịch vụ hiện hành
    List<Map<String, Object>> getAllPlans();

    // Main Flow - Bước 7: Cập nhật thông tin gói dịch vụ (Tên, Giá)
    void updatePlan(String id, UpdatePlanRequest request);

}