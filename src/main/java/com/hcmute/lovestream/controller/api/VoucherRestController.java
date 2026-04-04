package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.response.PublicVoucherResponse;
import com.hcmute.lovestream.entity.Voucher;
import com.hcmute.lovestream.repository.VoucherRepository;
import com.hcmute.lovestream.service.voucher.VoucherCheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherRestController {

    private final VoucherRepository voucherRepository;
    private final VoucherCheckoutService voucherCheckoutService;

    @GetMapping("/available")
    public ResponseEntity<List<PublicVoucherResponse>> getAvailableVouchers() {
        List<Voucher> vouchers = voucherRepository.findAvailableVouchers();
        List<PublicVoucherResponse> responses = vouchers.stream()
                .map(v -> PublicVoucherResponse.builder()
                        .code(v.getCode())
                        .discountPercent(v.getDiscountPercent())
                        .expiryDate(v.getExpiryDate())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateVoucher(@org.springframework.web.bind.annotation.RequestParam String code,
                                             @org.springframework.web.bind.annotation.RequestParam java.math.BigDecimal planPrice) {
        try {
            com.hcmute.lovestream.dto.response.VoucherQuoteResponse quote = voucherCheckoutService.validateAndCompute(code, planPrice);
            return ResponseEntity.ok(quote);
        } catch(RuntimeException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        }
    }
}
