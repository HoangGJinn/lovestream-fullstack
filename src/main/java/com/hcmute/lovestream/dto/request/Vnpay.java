package com.hcmute.lovestream.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Vnpay {
    private String amount;
    private String orderInfo;
    private String orderId; // ID đơn hàng từ hệ thống của bạn
}