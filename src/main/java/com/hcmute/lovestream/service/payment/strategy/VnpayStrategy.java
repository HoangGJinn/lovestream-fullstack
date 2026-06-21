package com.hcmute.lovestream.service.payment.strategy;

import com.hcmute.lovestream.dto.request.Vnpay;
import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

/**
 * Adapter và Concrete Strategy cho cổng thanh toán VNPAY.
 * Đóng vai trò là Adapter để tương thích với VnpayService (Adaptee).
 */
@Component
@RequiredArgsConstructor
public class VnpayStrategy implements PaymentGatewayStrategy {

    private final VnpayService vnpayService;

    @Override
    public String createPaymentUrl(Payment payment, String orderInfo, HttpServletRequest request) {
        // Build DTO đặc thù của VNPAY từ thực thể Payment chung
        Vnpay paymentRequest = Vnpay.builder()
                .amount(payment.getAmount().stripTrailingZeros().toPlainString())
                .orderInfo(orderInfo)
                .orderId(payment.getTransactionCode())
                .build();

        try {
            // Gọi Adaptee
            return vnpayService.createPaymentWithOrderCode(paymentRequest, payment.getTransactionCode(), request);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Không thể tạo link thanh toán VNPay: Lỗi mã hóa URL", e);
        }
    }

    @Override
    public String handleCallback(HttpServletRequest request) {
        // Delegate trực tiếp cho Adaptee
        return vnpayService.handlePaymentCallback(request);
    }
}
