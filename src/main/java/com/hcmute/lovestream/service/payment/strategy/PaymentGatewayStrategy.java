package com.hcmute.lovestream.service.payment.strategy;

import com.hcmute.lovestream.entity.Payment;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Giao diện định nghĩa chiến lược (Strategy) và mục tiêu (Target) cho thanh toán.
 * Áp dụng kết hợp Strategy Pattern và Adapter Pattern.
 */
public interface PaymentGatewayStrategy {
    
    /**
     * Tạo URL thanh toán chuyển hướng tới cổng thanh toán.
     *
     * @param payment   Thông tin thanh toán (chứa số tiền, mã giao dịch, user...)
     * @param orderInfo Thông tin mô tả đơn hàng (cần cho VNPay)
     * @param request   HTTP Request hiện tại (để lấy IP, URL gốc...)
     * @return Đường dẫn URL thanh toán
     */
    String createPaymentUrl(Payment payment, String orderInfo, HttpServletRequest request);

    /**
     * Xử lý kết quả trả về từ cổng thanh toán (Callback / Webhook / Return URL).
     *
     * @param request HTTP Request chứa các tham số trả về từ cổng thanh toán
     * @return Payment ID nếu thanh toán thành công, hoặc null nếu thất bại / lỗi chữ ký.
     */
    String handleCallback(HttpServletRequest request);
}
