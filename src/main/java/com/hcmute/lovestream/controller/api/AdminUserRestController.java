package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.admin.user.CreateContentManagerRequest;
import com.hcmute.lovestream.service.admin.user.AdminUserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserRestController {

    // Tiêm Service của bạn vào đây để gọi logic Database
    private final AdminUserManagementService adminUserManagementService;
    private final AdminUserManagementService adminUserService;

    // 1. Lấy danh sách Người dùng (Bổ sung lúc nãy)
    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminUserManagementService.getAllUsers(search));
    }

    // 2. TẠO TÀI KHOẢN CM - ĐÂY CHÍNH LÀ CÁI HỆ THỐNG ĐANG BÁO "NOT FOUND"
    @PostMapping("/content-manager")
    public ResponseEntity<?> createContentManager(@Valid @RequestBody CreateContentManagerRequest request) {
        try {
            // Gọi xuống Service để mã hóa pass và lưu DB
            adminUserManagementService.createContentManager(request);
            return ResponseEntity.ok(Map.of("message", "Tạo tài khoản Content Manager thành công!"));
        } catch (RuntimeException e) {
            // Nếu trùng Email hoặc SĐT, báo lỗi 400 Bad Request về cho Frontend hiện chữ đỏ
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Khóa / Mở khóa người dùng (Để dành làm sau nếu cần)
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái cho user ID: " + id));
    }

    // Bắt lỗi Validation (bỏ trống ô, sai định dạng email...) từ DTO
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("error", errorMessage));
    }
    // API Khóa / Mở khóa tài khoản User
    @PutMapping("/{id}/toggle-lock")
    public ResponseEntity<?> toggleUserLock(@PathVariable String id, @RequestBody(required = false) Map<String, String> payload) {
        try {
            String reason = payload != null ? payload.get("reason") : "";
            adminUserService.toggleUserLock(id, reason);
            return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}