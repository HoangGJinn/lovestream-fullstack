package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.*;
import com.hcmute.lovestream.service.authentication.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthService authService;

    // UC1: Đăng ký
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Register request) {
        try {
            authService.register(request);
            return ResponseEntity.ok(Map.of("message", "Đăng ký thành công. Vui lòng kiểm tra email để xác nhận."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // UC2: Xác nhận Email
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmail request) { // Đã đổi sang dùng DTO
        try {
            authService.verifyEmail(request.getToken());
            return ResponseEntity.ok(Map.of("message", "Xác nhận email thành công. Bạn có thể đăng nhập."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // UC3: Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login request, HttpServletResponse response) {
        try {
            // Lấy Map chứa cả 2 token từ Service
            Map<String, String> tokens = authService.login(request);

            // 1. Nhét Access Token vào Cookie (Tên cookie trùng với cấu hình trong JwtUtil)
            Cookie accessCookie = new Cookie("JWT_TOKEN", tokens.get("accessToken"));
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            // Ở properties bạn để 86400000ms (1 ngày). Cookie tính bằng giây nên chia cho 1000
            accessCookie.setMaxAge(86400);
            response.addCookie(accessCookie);

            // 2. Nhét Refresh Token vào Cookie
            Cookie refreshCookie = new Cookie("REFRESH_TOKEN", tokens.get("refreshToken"));
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            // Ở properties bạn để 604800000ms (7 ngày) -> 604800 giây
            refreshCookie.setMaxAge(604800);
            response.addCookie(refreshCookie);

            return ResponseEntity.ok(Map.of(
                    "message", "Đăng nhập thành công",
                    "redirectUrl", "/home"
            ));
        } catch (Exception e) {
            if ("Tài khoản chưa được xác minh email".equals(e.getMessage())) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", e.getMessage(),
                        "isUnverified", true
                ));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // UC4: Quên mật khẩu (Gửi email)
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPassword request) { // Đã đổi sang dùng DTO
        try {
            authService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "Mã xác nhận đã được gửi đến email."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // UC4: Đặt lại mật khẩu (Sau khi nhập mã OTP)
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPassword request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // UC5: Đăng xuất
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Xóa Cookie JWT
        Cookie cookie = new Cookie("JWT_TOKEN", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Set tuổi thọ cookie về 0 để xóa
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công", "redirectUrl", "/"));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> payload) {
        try {
            authService.resendOtp(payload.get("email"));
            return ResponseEntity.ok(Map.of("message", "Mã xác nhận mới đã được gửi."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshTokenString,
            HttpServletResponse response) {

        // Kiểm tra xem Cookie có tồn tại Refresh Token không
        if (refreshTokenString == null || refreshTokenString.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Không tìm thấy Refresh Token. Vui lòng đăng nhập lại."));
        }

        try {
            // Gọi xuống Service để đổi token mới
            String newAccessToken = authService.refreshToken(refreshTokenString);

            // Ghi đè Access Token mới vào Cookie
            Cookie accessCookie = new Cookie("JWT_TOKEN", newAccessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(86400); // Tùy chỉnh lại thời gian theo properties (đang tính bằng giây)
            response.addCookie(accessCookie);

            return ResponseEntity.ok(Map.of("message", "Đã gia hạn phiên đăng nhập thành công"));

        } catch (Exception e) {
            // Nếu Refresh Token lỗi, hết hạn hoặc bị thu hồi -> Báo lỗi 401 để Client đá về trang Login
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}