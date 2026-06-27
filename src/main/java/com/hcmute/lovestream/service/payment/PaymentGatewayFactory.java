package com.hcmute.lovestream.service.payment;

import com.hcmute.lovestream.entity.enums.PaymentGateway;
import com.hcmute.lovestream.service.payment.strategy.MomoStrategy;
import com.hcmute.lovestream.service.payment.strategy.PaymentGatewayStrategy;
import com.hcmute.lovestream.service.payment.strategy.VnpayStrategy;
import com.hcmute.lovestream.service.payment.strategy.ZalopayStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory cung cấp Strategy thích hợp dựa trên cổng thanh toán được chọn.
 */
@Component
@RequiredArgsConstructor
public class PaymentGatewayFactory {

    private final VnpayStrategy vnpayStrategy;
    private final MomoStrategy momoStrategy;
    private final ZalopayStrategy zalopayStrategy;

    public PaymentGatewayStrategy getStrategy(PaymentGateway gateway) {
        if (gateway == null) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        }
        
        return switch (gateway) {
            case VNPAY -> vnpayStrategy;
            case MOMO -> momoStrategy;
            case ZALOPAY -> zalopayStrategy;
            default -> throw new UnsupportedOperationException("Cổng thanh toán " + gateway + " chưa được hỗ trợ.");
        };
    }
}
