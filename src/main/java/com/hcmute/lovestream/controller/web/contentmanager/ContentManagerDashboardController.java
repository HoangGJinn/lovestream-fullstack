package com.hcmute.lovestream.controller.web.contentmanager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ContentManagerDashboardController {

    @GetMapping("/content-manager")
    public String index() {
        return "redirect:/content-manager/dashboard";
    }

    @GetMapping("/content-manager/dashboard")
    public String dashboard() {
        return "content-manager/dashboard";
    }
}
