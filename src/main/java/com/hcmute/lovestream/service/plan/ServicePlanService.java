package com.hcmute.lovestream.service.plan;

import com.hcmute.lovestream.dto.response.PurchaseResponse;
import com.hcmute.lovestream.dto.response.ServicePlanResponse;
import com.hcmute.lovestream.entity.ServicePlan;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface ServicePlanService {

    // Lấy tất cả gói đang kích hoạt, sắp xếp giá tăng dần
    List<ServicePlanResponse> getAllActivePlans();

    // Lấy chi tiết 1 gói (chỉ trả gói đang active)
    ServicePlanResponse getPlanById(String planId);

    // Mua gói: tạo Payment + Subscription, trả về thông tin đã kích hoạt
    PurchaseResponse purchasePlan(String userEmail, String planId, HttpServletRequest request);

    // Kiểm tra user có đang có gói ACTIVE không (dùng để hiển thị badge trên UI)
    boolean hasActiveSubscription(String userEmail);
}
