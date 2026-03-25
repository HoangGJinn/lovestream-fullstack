package com.hcmute.lovestream.controller.web.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    // CHỈ CẦN ĐÚNG 1 HÀM NÀY ĐỂ TRẢ VỀ GIAO DIỆN HTML MỚI (Trang chứa bảng và Modal)
    @GetMapping
    public String userManagePage(Model model) {
        model.addAttribute("pageTitle", "Quản lý Người dùng");
        return "admin/admin-users";
    }
}