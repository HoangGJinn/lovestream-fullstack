package com.hcmute.lovestream.service.voucher;

import com.hcmute.lovestream.entity.Voucher;
import com.hcmute.lovestream.entity.enums.VoucherStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/**
 * Quy tắc nghiệp vụ voucher đã chuẩn hoá (Bước 2 checklist). Dùng chung cho validate, tính giá và UI.
 * Chi tiết: {@code com/hcmute/lovestream/docs/Nhan/Voucher_Payment/02-voucher-business-rules.md}.
 */
public final class VoucherBusinessRules {

    /** Làm tròn tiền VND: đơn vị đồng, HALF_UP — thống nhất với VNPAY / Payment.amount. */
    public static final RoundingMode AMOUNT_ROUNDING = RoundingMode.HALF_UP;

    public static final int VND_SCALE = 0;

    /** Phần trăm giảm tối thiểu (0 = không giảm). */
    public static final int MIN_DISCOUNT_PERCENT = 0;

    /** Phần trăm giảm tối đa trong entity; 100% → tiền phải trả 0 — không hợp lệ với VNPAY. */
    public static final int MAX_DISCOUNT_PERCENT = 100;

    private VoucherBusinessRules() {
    }

    /**
     * Chuẩn hoá mã voucher: trim + uppercase (locale root) để so khớp mã trong DB.
     */
    public static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? trimmed : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * Voucher có được phép dùng tại ngày {@code today} hay không (ACTIVE, còn hạn, còn lượt).
     */
    public static boolean isEligibleForUse(Voucher voucher, LocalDate today) {
        Objects.requireNonNull(today, "today");
        if (voucher == null) {
            return false;
        }
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            return false;
        }
        LocalDate expiry = voucher.getExpiryDate();
        if (expiry == null || today.isAfter(expiry)) {
            return false;
        }
        Integer used = voucher.getUsedQuantity();
        Integer total = voucher.getTotalQuantity();
        if (used == null || total == null) {
            return false;
        }
        return used < total;
    }

    /**
     * Số tiền khách phải trả sau khi áp {@code discountPercent}% giảm trên {@code planPrice}.
     * Công thức: planPrice × (100 - discountPercent) / 100, làm tròn {@link #AMOUNT_ROUNDING}.
     *
     * @throws IllegalArgumentException nếu giá âm/null hoặc phần trăm ngoài [0, 100]
     */
    public static BigDecimal computePayableAmount(BigDecimal planPrice, int discountPercent) {
        Objects.requireNonNull(planPrice, "planPrice");
        if (planPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá gói không được âm");
        }
        if (discountPercent < MIN_DISCOUNT_PERCENT || discountPercent > MAX_DISCOUNT_PERCENT) {
            throw new IllegalArgumentException(
                    "discountPercent phải trong [" + MIN_DISCOUNT_PERCENT + ", " + MAX_DISCOUNT_PERCENT + "]");
        }
        BigDecimal hundred = BigDecimal.valueOf(100);
        BigDecimal factor = hundred.subtract(BigDecimal.valueOf(discountPercent)).divide(hundred, 10, AMOUNT_ROUNDING);
        return planPrice.multiply(factor).setScale(VND_SCALE, AMOUNT_ROUNDING);
    }

    /**
     * Sau giảm giá, số tiền có thể gửi VNPAY hay không (VNPAY yêu cầu &gt; 0).
     */
    public static boolean isPayableThroughVnpay(BigDecimal payableAmount) {
        return payableAmount != null && payableAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}
