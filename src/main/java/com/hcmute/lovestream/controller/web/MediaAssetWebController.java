package com.hcmute.lovestream.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MediaAssetWebController {
    @GetMapping("/watch/{movieId}")
    public String moviePage(@org.springframework.web.bind.annotation.PathVariable String movieId, org.springframework.ui.Model model) {
        model.addAttribute("movieId", movieId);
        return "videocontent/watch_movie";
    }
}
