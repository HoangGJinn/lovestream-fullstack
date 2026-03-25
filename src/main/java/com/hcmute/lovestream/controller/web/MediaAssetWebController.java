package com.hcmute.lovestream.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MediaAssetWebController {
    @GetMapping("/watch-movie")
    public String moviePage(
            @RequestParam String id,
            @RequestParam(required = false) String roomCode,
            Model model
    ) {
        model.addAttribute("videoId", id);
        model.addAttribute("roomCode", roomCode);
        return "videocontent/watch_movie";
    }

}
