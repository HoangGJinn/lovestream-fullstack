package com.hcmute.lovestream.service.plan;

import com.hcmute.lovestream.dto.response.PurchaseResponse;
import com.hcmute.lovestream.dto.response.ServicePlanResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface ServicePlanService {

    // Lấy tất cả gói đang kích hoạt, sắp xếp giá tăng dần
    List<ServicePlanResponse> getAllActivePlans();

    // Lấy chi tiết 1 gói (chỉ trả gói đang active)
    ServicePlanResponse getPlanById(String planId);

    // Mua gói: tạo Payment + Subscription, trả về thông tin đã kích hoạt
    PurchaseResponse purchasePlan(String userEmail, String planId, HttpServletRequest request);

    // Lấy các gói có giá cao hơn gói hiện tại của user để phục vụ nâng cấp
    List<ServicePlanResponse> getAvailableUpgradePlans(String userEmail);

    // Nâng cấp gói: thanh toán phần chênh lệch để chuyển sang gói cao hơn
    PurchaseResponse upgradePlan(String userEmail, String targetPlanId, HttpServletRequest request);

    // Kiểm tra user có đang có gói ACTIVE không (dùng để hiển thị badge trên UI)
    boolean hasActiveSubscription(String userEmail);
}
