package com.hcmute.lovestream.service.admin;

import com.hcmute.lovestream.dto.request.VoucherCreateRequest;

import java.util.List;
import java.util.Map;

public interface VoucherService {
    void createVoucher(VoucherCreateRequest request);
    List<Map<String, Object>> getAllVouchers(String search);

    void toggleVoucherStatus(String code);
    void duplicateVoucher(String sourceCode, String newCode);
}