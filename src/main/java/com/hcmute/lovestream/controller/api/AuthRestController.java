package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.*;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.service.authentication.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
    private final UserRepository userRepository; // Đã dọn lại import cho gọn

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
    public ResponseEntity<?> login(@Valid @RequestBody Login request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse response) {
        try {
            Map<String, String> tokens = authService.login(request, httpRequest.getHeader("User-Agent"));

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

            if (request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
                Cookie deviceCookie = new Cookie("DEVICE_ID", request.getDeviceId().trim());
                deviceCookie.setHttpOnly(false);
                deviceCookie.setPath("/");
                deviceCookie.setMaxAge(31536000);
                response.addCookie(deviceCookie);
            }

            // KẾT HỢP LOGIC KIỂM TRA ROLE ĐỂ ĐIỀU HƯỚNG
            String redirectUrl = "/home";

            // Lấy role từ token map (Cách tối ưu)
            String role = tokens.get("role");

            // Nếu map không có, fallback sang gọi Database (Cách an toàn của nhánh dev)
            if (role == null || role.isBlank()) {
                var userOpt = userRepository.findByEmail(request.getEmail());
                if (userOpt.isPresent()) {
                    role = userOpt.get().getRole().name();
                }
            }

            // Chuẩn hóa role về dạng authority chuẩn có chứa ROLE_
            if (role != null && !role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }

            // Nếu là Admin hoặc Content Manager thì cho vào Dashboard
            if ("ROLE_ADMIN".equals(role)) {
                redirectUrl = "/admin/dashboard";
            } else if ("ROLE_CONTENT_MANAGER".equals(role)) {
                redirectUrl = "/content-manager/dashboard";
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Đăng nhập thành công",
                    "redirectUrl", redirectUrl));
        } catch (Exception e) {
            if ("Tài khoản chưa được xác minh email".equals(e.getMessage())) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", e.getMessage(),
                        "isUnverified", true));
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
    @PostMapping("/verify-forgot-password-otp")
    public ResponseEntity<?> verifyForgotPasswordOtp(@Valid @RequestBody VerifyEmail request) {
        try {
            authService.verifyForgotPasswordOtp(request.getToken());
            return ResponseEntity.ok(Map.of("message", "Xác minh OTP thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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
            HttpServletRequest request,
            HttpServletResponse response) {

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        // Xóa JWT cookie
        Cookie jwtCookie = new Cookie("JWT_TOKEN", "");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        // Xóa Refresh Token cookie
        Cookie refreshCookie = new Cookie("REFRESH_TOKEN", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

        Cookie deviceCookie = new Cookie("DEVICE_ID", "");
        deviceCookie.setHttpOnly(false);
        deviceCookie.setPath("/");
        deviceCookie.setMaxAge(0);
        response.addCookie(deviceCookie);

        // Xóa OAuth2 state cookie (nếu user đang ở giữa luồng đăng nhập Google)
        Cookie oauth2Cookie = new Cookie("oauth2_auth_request", "");
        oauth2Cookie.setHttpOnly(true);
        oauth2Cookie.setPath("/");
        oauth2Cookie.setMaxAge(0);
        response.addCookie(oauth2Cookie);

        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công", "redirectUrl", "/login"));
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
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Không tìm thấy Refresh Token. Vui lòng đăng nhập lại."));
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
