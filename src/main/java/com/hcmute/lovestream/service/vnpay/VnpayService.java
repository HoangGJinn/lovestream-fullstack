package com.hcmute.lovestream.service.vnpay;

import com.hcmute.lovestream.dto.request.Vnpay;
import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

public interface VnpayService {
    String createPayment(Vnpay paymentRequest, HttpServletRequest request) throws UnsupportedEncodingException;

    String createPaymentWithOrderCode(Vnpay paymentRequest, String orderCode, HttpServletRequest request) throws UnsupportedEncodingException;

    // Xử lý kết quả trả về từ VNPAY và cập nhật Order
    // Trả về paymentId nếu thành công, null nếu thất bại hoặc không tìm thấy Payment
    String handlePaymentCallback(HttpServletRequest request);
}
