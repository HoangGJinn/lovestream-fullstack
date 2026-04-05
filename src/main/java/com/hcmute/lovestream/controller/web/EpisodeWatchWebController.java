package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.Episode;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.repository.EpisodeRepository;
import com.hcmute.lovestream.service.plan.ServicePlanService;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class EpisodeWatchWebController {

    private final EpisodeRepository episodeRepository;
    private final UserProfileService userProfileService;
    private final ServicePlanService servicePlanService;

    @Value("${app.stream-session.heartbeat-seconds:25}")
    private int streamHeartbeatSeconds;

    /**
     * GET /watch-episode?episodeId=...
     * Render trang player cho tập phim.
     * Video URL sẽ được fetch từ JS gọi /api/video/watch/episode/{episodeId}
     */
    @GetMapping("/watch-episode")
    public String watchEpisodePage(
            @RequestParam String episodeId,
            Authentication authentication,
            Model model
    ) {
        // 1. Fetch Episode and Series info
        Episode episode = episodeRepository.findById(episodeId).orElse(null);
        if (episode != null && episode.getSeason() != null && episode.getSeason().getTvSeries() != null) {
            model.addAttribute("videoId", episode.getSeason().getTvSeries().getId());
        }

        // 2. Fetch User Profile
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                User user = userProfileService.getCurrentUserByEmail(authentication.getName());
                model.addAttribute("currentUserEmail", user.getEmail());
                model.addAttribute("currentUserAvatar", user.getAvatar());
            } catch (RuntimeException e) {
                System.err.println("Error fetching user profile: " + e.getMessage());
            }
        }

        // 3. Service Plan and Quality settings
        String userEmail = authentication != null ? authentication.getName() : null;
        int maxAllowedHeight = servicePlanService.getMaxAllowedVideoHeight(userEmail);
        String planQualityLabel = servicePlanService.getCurrentPlanQualityLabel(userEmail);
        model.addAttribute("maxAllowedHeight", maxAllowedHeight);
        model.addAttribute("planQualityLabel", planQualityLabel);
        model.addAttribute("streamHeartbeatIntervalMs", Math.max(streamHeartbeatSeconds, 10) * 1000);

        model.addAttribute("episodeId", episodeId);
        return "videocontent/watch_episode";
    }
}

