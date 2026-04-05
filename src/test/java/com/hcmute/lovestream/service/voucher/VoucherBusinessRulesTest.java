package com.hcmute.lovestream.service.voucher;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoucherBusinessRulesTest {

    @Test
    void computePayableAmount_99k_discount40_percent_yields_59400() {
        BigDecimal plan = new BigDecimal("99000");
        BigDecimal payable = VoucherBusinessRules.computePayableAmount(plan, 40);
        assertEquals(new BigDecimal("59400"), payable);
    }

    @Test
    void normalizeCode_trimsAndUppercases() {
        assertEquals("SAVE40", VoucherBusinessRules.normalizeCode("  save40  "));
    }

    @Test
    void isPayableThroughVnpay_falseWhenZero() {
        assertFalse(VoucherBusinessRules.isPayableThroughVnpay(BigDecimal.ZERO));
        assertTrue(VoucherBusinessRules.isPayableThroughVnpay(new BigDecimal("1")));
    }
}
