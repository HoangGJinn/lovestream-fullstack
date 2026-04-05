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

    // CHỈ CẦN 1 BIẾN DUY NHẤT NÀY THÔI
    private final AdminUserManagementService adminUserManagementService;

    // 1. Lấy danh sách Người dùng
    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminUserManagementService.getAllUsers(search));
    }

    // 2. Tạo tài khoản Content Manager
    @PostMapping("/content-manager")
    public ResponseEntity<?> createContentManager(@Valid @RequestBody CreateContentManagerRequest request) {
        try {
            adminUserManagementService.createContentManager(request);
            return ResponseEntity.ok(Map.of("message", "Tạo tài khoản Content Manager thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Khóa / Mở khóa tài khoản User (ĐÃ XÓA HÀM CŨ, CHỈ GIỮ LẠI HÀM NÀY)
    @PutMapping("/{id}/toggle-lock")
    public ResponseEntity<?> toggleUserLock(@PathVariable String id, @RequestBody(required = false) Map<String, String> payload) {
        try {
            String reason = (payload != null && payload.containsKey("reason")) ? payload.get("reason") : "";

            // Gọi dùng đúng tên biến ở trên cùng
            adminUserManagementService.toggleUserLock(id, reason);

            return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 4. Bắt lỗi Validation từ DTO
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("error", errorMessage));
    }
}