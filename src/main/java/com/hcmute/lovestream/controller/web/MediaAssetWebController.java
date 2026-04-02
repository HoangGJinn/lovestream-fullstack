package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.response.WatchRoomStateResponse;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.service.user.UserProfileService;
import com.hcmute.lovestream.service.plan.ServicePlanService;
import com.hcmute.lovestream.service.watchtogether.WatchTogetherService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MediaAssetWebController {

    private final WatchTogetherService watchTogetherService;
    private final UserProfileService userProfileService;

    private final ServicePlanService servicePlanService;

    @Value("${app.stream-session.heartbeat-seconds:25}")
    private int streamHeartbeatSeconds;

    @GetMapping("/watch-movie")
    public String moviePage(
            @RequestParam String id,
            @RequestParam(required = false) String roomCode,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (roomCode != null && !roomCode.isBlank()) {
            if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
                return "redirect:/login";
            }
            try {
                WatchRoomStateResponse roomState = watchTogetherService.getRoomState(roomCode, authentication.getName());
                model.addAttribute("roomState", roomState);
            } catch (RuntimeException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/watch-together";
            }
        }

        // 2. Xử lý logic lấy thông tin người dùng (Qua UserProfileService)
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                User user = userProfileService.getCurrentUserByEmail(authentication.getName());
                model.addAttribute("currentUserEmail", user.getEmail());
                model.addAttribute("currentUserAvatar", user.getAvatar());
            } catch (RuntimeException e) {
                System.err.println("Error fetching user profile: " + e.getMessage());
            }
        }

        model.addAttribute("videoId", id);
        model.addAttribute("roomCode", roomCode);

        String userEmail = authentication != null ? authentication.getName() : null;
        int maxAllowedHeight = servicePlanService.getMaxAllowedVideoHeight(userEmail);
        String planQualityLabel = servicePlanService.getCurrentPlanQualityLabel(userEmail);
        model.addAttribute("maxAllowedHeight", maxAllowedHeight);
        model.addAttribute("planQualityLabel", planQualityLabel);
        model.addAttribute("streamHeartbeatIntervalMs", Math.max(streamHeartbeatSeconds, 10) * 1000);

        return "videocontent/watch_movie";
    }

}
