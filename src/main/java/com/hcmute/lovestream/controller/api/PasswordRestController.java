package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.BackupChangePasswordRequest;
import com.hcmute.lovestream.dto.request.ChangePasswordRequest;
import com.hcmute.lovestream.service.user.ChangePasswordService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/password")
@RequiredArgsConstructor
public class PasswordRestController {

    private final ChangePasswordService changePasswordService;

    @PostMapping("/change")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response
    ) {
        try {
            changePasswordService.changePasswordByOldPassword(authentication.getName(), request);
            clearAuthCookies(response);
            return ResponseEntity.ok(Map.of(
                    "message", "Đổi mật khẩu thành công. Vui lòng đăng nhập lại.",
                    "redirectUrl", "/login"
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/backup-change")
    public ResponseEntity<?> changePasswordByBackupLink(
            @Valid @RequestBody BackupChangePasswordRequest request,
            HttpServletResponse response
    ) {
        try {
            changePasswordService.changePasswordByBackupLink(request);
            clearAuthCookies(response);
            return ResponseEntity.ok(Map.of(
                    "message", "Đổi mật khẩu thành công. Vui lòng đăng nhập lại.",
                    "redirectUrl", "/login"
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private void clearAuthCookies(HttpServletResponse response) {
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
    }
}
