package com.hcmute.lovestream.controller.api;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hcmute.lovestream.service.admin.transaction.AdminPaymentService;
import com.hcmute.lovestream.service.admin.transaction.AdminReportService;
import com.hcmute.lovestream.service.admin.transaction.AdminReportService.ReportDocument;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentRestController {

    private final AdminPaymentService adminPaymentService;
    private final AdminReportService adminReportService;

    // API Lấy giao dịch có hỗ trợ Bộ Lọc
    @GetMapping("/filter")
    public ResponseEntity<?> filterPayments(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String status) {

        return ResponseEntity.ok(adminPaymentService.getFilteredTransactions(fromDate, toDate, plan, status));
    }

    // Export endpoint: pdf or xlsx
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPayments(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "pdf") String format
    ) {
        var data = adminPaymentService.getFilteredTransactions(fromDate, toDate, plan, status);
        ReportDocument doc = adminReportService.createReport(data, format);
        byte[] bytes = doc.content().toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(doc.contentType()));
        ContentDisposition cd = ContentDisposition.builder("attachment").filename(doc.fileName()).build();
        headers.setContentDisposition(cd);

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}