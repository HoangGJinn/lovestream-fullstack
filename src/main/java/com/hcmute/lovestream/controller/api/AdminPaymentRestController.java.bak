package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.service.admin.transaction.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentRestController {

    private final AdminPaymentService adminPaymentService;

    // API Lấy giao dịch có hỗ trợ Bộ Lọc
    @GetMapping("/filter")
    public ResponseEntity<?> filterPayments(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String status) {

        return ResponseEntity.ok(adminPaymentService.getFilteredTransactions(fromDate, toDate, plan, status));
    }
}