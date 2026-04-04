package com.hcmute.lovestream.service.voucher;

import com.hcmute.lovestream.dto.response.VoucherQuoteResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Validate mã voucher và tính giá cuối cho một gói (chỉ đọc DB, không tăng {@code usedQuantity}).
 */
public interface VoucherCheckoutService {

    ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    default VoucherQuoteResponse validateAndCompute(String voucherCode, BigDecimal planPrice) {
        return validateAndCompute(voucherCode, planPrice, LocalDate.now(VN_ZONE));
    }

    VoucherQuoteResponse validateAndCompute(String voucherCode, BigDecimal planPrice, LocalDate asOf);
}
