package com.hcmute.lovestream.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserWebController {

    @GetMapping("/profile")
    public String profilePage() {
        return "user/profile";
    }

    @GetMapping("/account")
    public String accountOverviewPage() {
        return "user/account"; // Trỏ tới file templates/user/account.html
    }
}