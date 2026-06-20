package com.hcmute.lovestream.service.payment.strategy;

import com.hcmute.lovestream.entity.Payment;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Adapter và Concrete Strategy cho cổng thanh toán ZALOPAY.
 * Hiện tại đang là class rỗng (Mock) chờ tích hợp API ZALOPAY trong tương lai.
 */
@Component
public class ZalopayStrategy implements PaymentGatewayStrategy {

    @Override
    public String createPaymentUrl(Payment payment, String orderInfo, HttpServletRequest request) {
        throw new UnsupportedOperationException("Cổng thanh toán ZaloPay hiện chưa được hỗ trợ.");
    }

    @Override
    public String handleCallback(HttpServletRequest request) {
        throw new UnsupportedOperationException("Cổng thanh toán ZaloPay hiện chưa được hỗ trợ.");
    }
}
