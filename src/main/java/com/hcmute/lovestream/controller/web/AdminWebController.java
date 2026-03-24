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

    // ĐÃ XÓA HÀM @GetMapping("/users") Ở ĐÂY VÌ ĐÃ CÓ ADMIN_USER_CONTROLLER LO
}