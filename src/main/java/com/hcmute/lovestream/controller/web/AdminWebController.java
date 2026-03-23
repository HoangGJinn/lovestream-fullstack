package com.hcmute.lovestream.controller.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
// @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')") // Mở comment này nếu bạn dùng Method Security
public class AdminWebController {

    // Trả về trang Dashboard thống kê
    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard - Netflix Clone");
        return "admin/admin-dashboard";
    }

    // Tiện tay tạo sẵn các endpoint cho các Use Case khác luôn
    @GetMapping("/vouchers")
    public String voucherManagePage(Model model) {
        model.addAttribute("pageTitle", "Quản lý Voucher");
        return "admin/admin-vouchers";
    }

    @GetMapping("/users")
    public String userManagePage(Model model) {
        model.addAttribute("pageTitle", "Quản lý Người dùng");
        return "admin/admin-users";
    }
}