package com.hcmute.lovestream.controller.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminContentManagerWebController {

    @GetMapping("/admin/users/content-managers")
    public String contentManagersPage() {
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/content-managers/new")
    public String newContentManagerPage() {
        return "redirect:/admin/users";
    }
}
