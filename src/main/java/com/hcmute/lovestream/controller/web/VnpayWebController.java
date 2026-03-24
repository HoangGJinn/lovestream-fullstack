package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import com.hcmute.lovestream.repository.PaymentRepository;
import com.hcmute.lovestream.repository.SubscriptionRepository;
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

@Controller
@RequiredArgsConstructor
public class VnpayWebController {

    private final VnpayService vnpayService;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final JwtUtil jwtUtil;

    // Endpoint callback mà VNPay gọi về theo cấu hình `vnp.return.url`
    @GetMapping("/v1/api/vnpay/payment-callback")
    public String paymentCallback(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication,
            Model model
    ) {
        String paymentId = vnpayService.handlePaymentCallback(request);
        boolean success = paymentId != null;

        model.addAttribute("success", success);
        model.addAttribute("paymentId", paymentId);
        model.addAttribute("message", success ? "Thanh toán thành công!" : "Thanh toán thất bại!");

        if (success) {
            paymentRepository.findById(paymentId).ifPresent(payment -> {
                addPaymentToModel(model, payment);
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

    private void refreshVipClaimCookie(Authentication authentication, HttpServletResponse response, Payment payment) {
        if (authentication == null || !authentication.isAuthenticated() || payment.getUser() == null) {
            return;
        }
        if (!payment.getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
            return;
        }
        boolean isVip = subscriptionRepository.existsByUser_IdAndStatusAndEndDateAfter(
                payment.getUser().getId(),
                SubscriptionStatus.ACTIVE,
                java.time.LocalDateTime.now()
        );
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("ROLE_USER");
        String token = jwtUtil.generateToken(payment.getUser(), isVip);
        Cookie jwtCookie = new Cookie("JWT_TOKEN", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(86400);
        response.addCookie(jwtCookie);
    }
}

