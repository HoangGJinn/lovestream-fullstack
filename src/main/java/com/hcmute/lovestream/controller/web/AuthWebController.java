package com.hcmute.lovestream.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthWebController {

    // Trả về trang giao diện đăng nhập
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login"; // Trỏ tới file src/main/resources/templates/auth/login.html
    }

    // Trả về trang giao diện đăng ký
    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    // Trả về trang quên mật khẩu
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    // Trả về trang xác nhận email
    @GetMapping("/verify-email")
    public String verifyEmailPage() {
        return "auth/verify-email";
    }
}