package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.repository.PaymentRepository;
import com.hcmute.lovestream.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class VnpayWebController {

    private final VnpayService vnpayService;
    private final PaymentRepository paymentRepository;

    // Endpoint callback mà VNPay gọi về theo cấu hình `vnp.return.url`
    @GetMapping("/v1/api/vnpay/payment-callback")
    public String paymentCallback(HttpServletRequest request, Model model) {
        String responseCode = request.getParameter("vnp_ResponseCode");
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
            paymentRepository.findById(paymentId).ifPresent(payment -> addPaymentToModel(model, payment));
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
}

