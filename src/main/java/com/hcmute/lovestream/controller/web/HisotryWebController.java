package com.hcmute.lovestream.controller.web;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HisotryWebController {
    @GetMapping("/history")
    public String history() {
        return "history/history";
    }
}
