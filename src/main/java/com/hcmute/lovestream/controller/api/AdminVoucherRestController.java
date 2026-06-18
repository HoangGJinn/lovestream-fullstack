package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.VoucherCreateRequest;
import com.hcmute.lovestream.service.admin.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
public class AdminVoucherRestController {

    private final VoucherService voucherService;

    // 1. API Lấy danh sách (Có hỗ trợ tìm kiếm) - ĐÃ SỬA: Lấy từ DB thật
    @GetMapping
    public ResponseEntity<?> getAllVouchers(@RequestParam(required = false) String search) {
        // Gọi thẳng xuống Service để lấy dữ liệu từ MySQL thay vì tự tạo list ảo
        return ResponseEntity.ok(voucherService.getAllVouchers(search));
    }

    // 2. API Tạo Voucher
    @PostMapping
    public ResponseEntity<?> createVoucher(@Valid @RequestBody VoucherCreateRequest request) {
        try {
            voucherService.createVoucher(request);
            return ResponseEntity.ok(Map.of("message", "Tạo voucher " + request.getCode().toUpperCase() + " thành công!"));
        } catch (Exception e) {
            // Trả về lỗi 400 Bad Request nếu bị trùng mã hoặc lỗi logic
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 3. API Khóa / Mở khóa
    @PutMapping("/{code}/toggle-status")
    public ResponseEntity<?> toggleVoucherStatus(@PathVariable String code) {
        try {
            // Gọi xuống Service để xử lý logic
            voucherService.toggleVoucherStatus(code);
            return ResponseEntity.ok(Map.of("message", "Đã thay đổi trạng thái của Voucher: " + code));
        } catch (Exception e) {
            // Bắt lỗi nếu mã Voucher không tồn tại
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Bắt lỗi Validation từ DTO và chuyển thành JSON báo cho Frontend
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        // Lấy câu thông báo lỗi đầu tiên (Ví dụ: "Mã voucher không được để trống")
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("error", errorMessage));
    }

    // 4. API Nhân bản Voucher (Ứng dụng Prototype Pattern)
    @PostMapping("/{code}/duplicate")
    public ResponseEntity<?> duplicateVoucher(@PathVariable String code, @RequestParam String newCode) {
        try {
            voucherService.duplicateVoucher(code, newCode);
            return ResponseEntity.ok(Map.of("message", "Đã nhân bản thành công Voucher mới: " + newCode.toUpperCase()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}