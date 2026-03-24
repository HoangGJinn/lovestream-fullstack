package com.hcmute.lovestream.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MediaAssetWebController {
    @GetMapping("/movie")
    public String moviePage() {
        return "videocontent/watch_movie";
    }
}
