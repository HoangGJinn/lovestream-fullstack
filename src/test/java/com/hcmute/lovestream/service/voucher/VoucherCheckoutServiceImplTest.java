package com.hcmute.lovestream.service.voucher;

import com.hcmute.lovestream.dto.response.VoucherQuoteResponse;
import com.hcmute.lovestream.entity.Voucher;
import com.hcmute.lovestream.entity.enums.VoucherStatus;
import com.hcmute.lovestream.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherCheckoutServiceImplTest {

    @Mock
    private VoucherRepository voucherRepository;

    @InjectMocks
    private VoucherCheckoutServiceImpl voucherCheckoutService;

    private final LocalDate today = LocalDate.of(2026, 6, 15);
    private final BigDecimal planPrice = new BigDecimal("99000");

    private Voucher activeVoucher;

    @BeforeEach
    void setUp() {
        activeVoucher = Voucher.builder()
                .id("v-id-1")
                .code("SAVE40")
                .discountPercent(40)
                .totalQuantity(100)
                .usedQuantity(0)
                .expiryDate(LocalDate.of(2026, 12, 31))
                .status(VoucherStatus.ACTIVE)
                .build();
    }

    @Test
    void validateAndCompute_emptyCode_throws() {
        assertThrows(RuntimeException.class,
                () -> voucherCheckoutService.validateAndCompute("   ", planPrice, today));
    }

    @Test
    void validateAndCompute_notFound_throws() {
        when(voucherRepository.findByCode("MISSING")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> voucherCheckoutService.validateAndCompute("missing", planPrice, today));
    }

    @Test
    void validateAndCompute_ineligible_throws() {
        activeVoucher.setStatus(VoucherStatus.BLOCKED);
        when(voucherRepository.findByCode("SAVE40")).thenReturn(Optional.of(activeVoucher));
        assertThrows(RuntimeException.class,
                () -> voucherCheckoutService.validateAndCompute("save40", planPrice, today));
    }

    @Test
    void validateAndCompute_expired_throws() {
        when(voucherRepository.findByCode("SAVE40")).thenReturn(Optional.of(activeVoucher));
        LocalDate afterExpiry = LocalDate.of(2027, 1, 1);
        assertThrows(RuntimeException.class,
                () -> voucherCheckoutService.validateAndCompute("save40", planPrice, afterExpiry));
    }

    @Test
    void validateAndCompute_success_returnsQuote() {
        when(voucherRepository.findByCode("SAVE40")).thenReturn(Optional.of(activeVoucher));

        VoucherQuoteResponse q = voucherCheckoutService.validateAndCompute("save40", planPrice, today);

        assertEquals("v-id-1", q.getVoucherId());
        assertEquals("SAVE40", q.getCode());
        assertEquals(40, q.getDiscountPercent());
        assertEquals(planPrice, q.getPlanPrice());
        assertEquals(new BigDecimal("59400"), q.getFinalAmount());
    }

    @Test
    void validateAndCompute_fullDiscount_throws() {
        activeVoucher.setDiscountPercent(100);
        when(voucherRepository.findByCode("SAVE40")).thenReturn(Optional.of(activeVoucher));
        assertThrows(RuntimeException.class,
                () -> voucherCheckoutService.validateAndCompute("save40", planPrice, today));
    }

    @Test
    void validateAndCompute_outOfUses_throws() {
        activeVoucher.setUsedQuantity(100);
        activeVoucher.setTotalQuantity(100);
        when(voucherRepository.findByCode("SAVE40")).thenReturn(Optional.of(activeVoucher));
        assertThrows(RuntimeException.class,
                () -> voucherCheckoutService.validateAndCompute("save40", planPrice, today));
    }

    @Test
    void validateAndCompute_nullPlanPrice_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> voucherCheckoutService.validateAndCompute("save40", null, today));
    }
}
