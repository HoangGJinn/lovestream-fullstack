package com.hcmute.lovestream.config.payment;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VnpayConfig {
    @Value("${vnp.pay.url}")
    private String vnp_PayUrl;

    @Value("${vnp.return.url}")
    private String vnp_ReturnUrl;

    @Value("${vnp.tmn.code}")
    private String vnp_TmnCode;

    @Value("${vnp.secret.key}")
    private String secretKey;

    @Value("${vnp.version}")
    private String vnp_Version;

    @Value("${vnp.command}")
    private String vnp_Command;

    @Value("${vnp.order.type}")
    private String orderType;
}