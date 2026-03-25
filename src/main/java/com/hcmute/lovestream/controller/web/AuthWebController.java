package com.hcmute.lovestream.controller.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthWebController {

    // Kiểm tra xem user đã đăng nhập chưa (Sử dụng logic chuẩn bảo mật nhất)
    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    // Helper trích xuất redirect URL dựa trên Role của user
    private String getRedirectUrlBasedOnRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) {
                return "redirect:/admin/dashboard";
            }

            boolean isContentManager = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CONTENT_MANAGER"));
            if (isContentManager) {
                return "redirect:/admin/movies";
            }
        }
        return "redirect:/home";
    }

    // Trả về trang giao diện đăng nhập
    @GetMapping("/login")
    public String loginPage() {
        if (isAuthenticated()) return getRedirectUrlBasedOnRole();
        return "auth/login";
    }

    // Trả về trang giao diện đăng ký
    @GetMapping("/register")
    public String registerPage() {
        if (isAuthenticated()) return getRedirectUrlBasedOnRole();
        return "auth/register";
    }

    // Trả về trang quên mật khẩu
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        if (isAuthenticated()) return getRedirectUrlBasedOnRole();
        return "auth/forgot-password";
    }

    // Trả về trang xác nhận email
    @GetMapping("/verify-email")
    public String verifyEmailPage() {
        if (isAuthenticated()) return getRedirectUrlBasedOnRole();
        return "auth/verify-email";
    }
}
