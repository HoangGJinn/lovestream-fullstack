package com.hcmute.lovestream.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Kết quả validate voucher + tính giá (Bước 3). Không thay đổi {@code usedQuantity} trên entity.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherQuoteResponse {

    private String voucherId;
    /** Mã đã chuẩn hoá (uppercase), khớp DB. */
    private String code;
    private Integer discountPercent;
    /** Giá gói đầu vào (VND). */
    private BigDecimal planPrice;
    /** Số tiền phải trả sau giảm (VND), làm tròn theo {@link com.hcmute.lovestream.service.voucher.VoucherBusinessRules}. */
    private BigDecimal finalAmount;
}
