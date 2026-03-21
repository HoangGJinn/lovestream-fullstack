package com.hcmute.lovestream.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthWebController {

    // Kiểm tra xem user đã đăng nhập chưa
    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    // Trả về trang giao diện đăng nhập
    @GetMapping("/login")
    public String loginPage() {
        if (isAuthenticated()) return "redirect:/home";
        return "auth/login";
    }

    // Trả về trang giao diện đăng ký
    @GetMapping("/register")
    public String registerPage() {
        if (isAuthenticated()) return "redirect:/home";
        return "auth/register";
    }

    // Trả về trang quên mật khẩu
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        if (isAuthenticated()) return "redirect:/home";
        return "auth/forgot-password";
    }

    // Trả về trang xác nhận email
    @GetMapping("/verify-email")
    public String verifyEmailPage() {
        if (isAuthenticated()) return "redirect:/home";
        return "auth/verify-email";
    }
}