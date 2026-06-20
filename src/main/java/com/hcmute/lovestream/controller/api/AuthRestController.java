package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.*;
import com.hcmute.lovestream.ratelimit.RateLimitExceededException;
import com.hcmute.lovestream.ratelimit.proxy.RateLimitedAuthServiceProxy;
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

    private final AuthService authService;           // Được Spring inject RateLimitedAuthServiceProxy (@Primary)
    private final RateLimitedAuthServiceProxy authProxy; // Dùng trực tiếp cho các method có overload IP
    private final UserRepository userRepository; // Đã dọn lại import cho gọn

    /**
     * Trích xuất IP thực của client.
     * Hỗ trợ reverse proxy (Nginx, load balancer) qua header X-Forwarded-For.
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For có thể chứa nhiều IP: "client, proxy1, proxy2"
            // Lấy IP đầu tiên = IP thực của client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // UC1: Đăng ký — Rate limited: 5 lần / 1 giờ / IP
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Register request,
                                      HttpServletRequest httpRequest) {
        try {
            authProxy.register(request, extractClientIp(httpRequest));
            return ResponseEntity.ok(Map.of("message", "Đăng ký thành công. Vui lòng kiểm tra email để xác nhận."));
        } catch (RateLimitExceededException e) {
            throw e; // Để GlobalExceptionHandler xử lý → HTTP 429
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

    // UC3: Đăng nhập — Rate limited: 10 lần / 15 phút / IP
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse response) {
        try {
            Map<String, String> tokens = authProxy.login(request, httpRequest.getHeader("User-Agent"), extractClientIp(httpRequest));

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
        } catch (RateLimitExceededException e) {
            throw e; // Để GlobalExceptionHandler xử lý → HTTP 429
        } catch (Exception e) {
            if ("Ôi không, tài khoản chưa được xác minh email".equals(e.getMessage())
                    || "Tài khoản chưa được xác minh email".equals(e.getMessage())) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", e.getMessage(),
                        "isUnverified", true));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // UC4: Quên mật khẩu — Rate limited: 3 lần / 10 phút / IP
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPassword request,
                                            HttpServletRequest httpRequest) {
        try {
            authProxy.forgotPassword(request.getEmail(), extractClientIp(httpRequest));
            return ResponseEntity.ok(Map.of("message", "Mã xác nhận đã được gửi đến email."));
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // UC4: Xác minh OTP — Rate limited: 5 lần / 5 phút / IP (chống brute-force OTP)
    @PostMapping("/verify-forgot-password-otp")
    public ResponseEntity<?> verifyForgotPasswordOtp(@Valid @RequestBody VerifyEmail request,
                                                     HttpServletRequest httpRequest) {
        try {
            authProxy.verifyForgotPasswordOtp(request.getToken(), extractClientIp(httpRequest));
            return ResponseEntity.ok(Map.of("message", "Xác minh OTP thành công."));
        } catch (RateLimitExceededException e) {
            throw e;
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

    // UC2: Gửi lại OTP — Rate limited: 3 lần / 10 phút / IP (chống spam email)
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> payload,
                                       HttpServletRequest httpRequest) {
        try {
            authProxy.resendOtp(payload.get("email"), extractClientIp(httpRequest));
            return ResponseEntity.ok(Map.of("message", "Mã xác nhận mới đã được gửi."));
        } catch (RateLimitExceededException e) {
            throw e;
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
