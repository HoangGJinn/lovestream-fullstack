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
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmail request) {
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
            // Lấy Map chứa token (và Role) từ Service
            Map<String, String> tokens = authService.login(request);

            // 1. Nhét Access Token vào Cookie
            Cookie accessCookie = new Cookie("JWT_TOKEN", tokens.get("accessToken"));
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(86400);
            response.addCookie(accessCookie);

            // 2. Nhét Refresh Token vào Cookie
            Cookie refreshCookie = new Cookie("REFRESH_TOKEN", tokens.get("refreshToken"));
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(604800);
            response.addCookie(refreshCookie);

            // 3. KIỂM TRA ROLE VÀ ĐIỀU HƯỚNG URL (Phần đã được sửa)
            String role = tokens.getOrDefault("role", "USER"); // Lấy role từ Map ra, mặc định là USER nếu không thấy
            String targetUrl = role.equals("ADMIN") ? "/admin/dashboard" : "/home"; // Nếu là Admin thì đi đường khác

            return ResponseEntity.ok(Map.of(
                    "message", "Đăng nhập thành công",
                    "redirectUrl", targetUrl // Nhét targetUrl động vào thay vì fix cứng "/home"
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
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPassword request) {
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
    public ResponseEntity<?> logout(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        Cookie jwtCookie = new Cookie("JWT_TOKEN", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        Cookie refreshCookie = new Cookie("REFRESH_TOKEN", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

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

        if (refreshTokenString == null || refreshTokenString.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Không tìm thấy Refresh Token. Vui lòng đăng nhập lại."));
        }

        try {
            Map<String, String> tokens = authService.refreshToken(refreshTokenString);

            Cookie accessCookie = new Cookie("JWT_TOKEN", tokens.get("accessToken"));
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(86400);
            response.addCookie(accessCookie);

            Cookie refreshCookie = new Cookie("REFRESH_TOKEN", tokens.get("refreshToken"));
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(604800);
            response.addCookie(refreshCookie);

            return ResponseEntity.ok(Map.of("message", "Đã gia hạn phiên đăng nhập thành công"));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}