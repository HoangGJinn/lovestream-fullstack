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



    // Quản lý Voucher
    @GetMapping("/vouchers")
    public String voucherManagePage(Model model) {
        model.addAttribute("pageTitle", "Quản lý Voucher");
        return "admin/admin-vouchers";
    }

    // Mở trang Quản lý Giao dịch
    @GetMapping("/transactions")
    public String transactionManagePage(Model model) {
        model.addAttribute("pageTitle", "Quản lý Giao dịch");
        return "admin/admin-transactions";
    }

    @GetMapping("/plans")
    public String planManagePage(Model model) {
        model.addAttribute("pageTitle", "Quản lý Gói dịch vụ");
        return "admin/admin-plans";
    }
}