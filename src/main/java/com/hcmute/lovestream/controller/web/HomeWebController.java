package com.hcmute.lovestream.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeWebController {

    @GetMapping({"/", "/home"})
    public String homePage() {
        return "home"; // Trỏ tới file templates/home.html
    }
}