package com.hcmute.lovestream.service.voucher;

import com.hcmute.lovestream.dto.response.VoucherQuoteResponse;
import com.hcmute.lovestream.entity.Voucher;
import com.hcmute.lovestream.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VoucherCheckoutServiceImpl implements VoucherCheckoutService {

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional(readOnly = true)
    public VoucherQuoteResponse validateAndCompute(String voucherCode, BigDecimal planPrice, LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        if (planPrice == null) {
            throw new IllegalArgumentException("Giá gói không được để trống");
        }
        if (planPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá gói không được âm");
        }

        String normalized = VoucherBusinessRules.normalizeCode(voucherCode);
        if (normalized == null || normalized.isEmpty()) {
            throw new RuntimeException("Vui lòng nhập mã voucher");
        }

        Voucher voucher = voucherRepository.findByCode(normalized)
                .orElseThrow(() -> new RuntimeException("Mã voucher không tồn tại"));

        if (!VoucherBusinessRules.isEligibleForUse(voucher, asOf)) {
            throw new RuntimeException("Voucher đã hết hạn, đã khóa hoặc hết lượt sử dụng");
        }

        int percent = voucher.getDiscountPercent() != null ? voucher.getDiscountPercent() : 0;
        BigDecimal finalAmount = VoucherBusinessRules.computePayableAmount(planPrice, percent);

        if (!VoucherBusinessRules.isPayableThroughVnpay(finalAmount)) {
            throw new RuntimeException("Voucher giảm 100% không thể thanh toán qua VNPAY");
        }

        return VoucherQuoteResponse.builder()
                .voucherId(voucher.getId())
                .code(voucher.getCode())
                .discountPercent(percent)
                .planPrice(planPrice)
                .finalAmount(finalAmount)
                .build();
    }
}
