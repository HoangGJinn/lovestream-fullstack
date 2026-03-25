package com.hcmute.lovestream.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EpisodeWatchWebController {

    /**
     * GET /watch-episode?episodeId=...
     * Render trang player cho t\u1eadp phim.
     * Video URL s\u1ebd \u0111\u01b0\u1ee3c fetch t\u1eeb JS g\u1ecdi /api/video/watch/episode/{episodeId}
     */
    @GetMapping("/watch-episode")
    public String watchEpisodePage(@RequestParam String episodeId, Model model) {
        model.addAttribute("episodeId", episodeId);
        return "videocontent/watch_episode";
    }
}
