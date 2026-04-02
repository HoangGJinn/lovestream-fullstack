package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import com.hcmute.lovestream.repository.PaymentRepository;
import com.hcmute.lovestream.repository.SubscriptionRepository;
import com.hcmute.lovestream.security.JwtAuthenticationFilter;
import com.hcmute.lovestream.security.JwtUtil;
import com.hcmute.lovestream.service.vnpay.VnpayService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class VnpayWebController {

    private final VnpayService vnpayService;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/v1/api/vnpay/payment-callback")
    public String paymentCallback(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication,
            Model model
    ) {
        // Lấy mã phản hồi từ VNPay
        String responseCode = request.getParameter("vnp_ResponseCode");
        
        // Xử lý logic thanh toán thông qua service
        String paymentId = vnpayService.handlePaymentCallback(request);
        
        boolean success = paymentId != null;
        boolean canceled = "24".equals(responseCode);

        String resultType = success ? "SUCCESS" : (canceled ? "CANCEL" : "FAILED");
        String message = success
                ? "Thanh toán thành công!"
                : (canceled ? "Bạn đã hủy giao dịch." : "Thanh toán không thành công. Vui lòng thử lại.");

        model.addAttribute("success", success);
        model.addAttribute("resultType", resultType);
        model.addAttribute("paymentId", paymentId);
        model.addAttribute("message", message);

        if (success) {
            paymentRepository.findById(paymentId).ifPresent(payment -> {
                addPaymentToModel(model, payment);
                // CẬP NHẬT QUYỀN VIP VÀO COOKIE NGAY LẬP TỨC
                refreshVipClaimCookie(authentication, response, payment);
            });
        }

        return "payment/vnpay-result";
    }

    private void addPaymentToModel(Model model, Payment payment) {
        model.addAttribute("transactionCode", payment.getTransactionCode());
        model.addAttribute("amount", payment.getAmount());
        if (payment.getServicePlan() != null) {
            model.addAttribute("planName", payment.getServicePlan().getName());
            model.addAttribute("durationDays", payment.getServicePlan().getDurationDays());
        }
        model.addAttribute("status", payment.getStatus());
    }

    /**
     * Cập nhật lại JWT Cookie chứa claim isVip mới mà không bắt user đăng nhập lại.
     */
    private void refreshVipClaimCookie(Authentication authentication, HttpServletResponse response, Payment payment) {
        if (authentication == null || !authentication.isAuthenticated() || payment.getUser() == null) {
            return;
        }

        // Kiểm tra xem user thanh toán có đúng là user đang đăng nhập không
        if (!payment.getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
            return;
        }

        // Kiểm tra trạng thái VIP mới nhất từ DB
        boolean isVip = subscriptionRepository.existsByUser_IdAndStatusAndEndDateAfter(
                payment.getUser().getId(),
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now()
        );

        String deviceId = null;
        if (authentication.getPrincipal() instanceof JwtAuthenticationFilter.JwtPrincipal principal) {
            Object principalDeviceId = principal.get("deviceId");
            if (principalDeviceId instanceof String value && !value.isBlank()) {
                deviceId = value;
            }
        }

        String token = jwtUtil.generateToken(payment.getUser(), isVip, deviceId);

        Cookie jwtCookie = new Cookie("JWT_TOKEN", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(86400); // 1 ngày
        response.addCookie(jwtCookie);
    }
}
