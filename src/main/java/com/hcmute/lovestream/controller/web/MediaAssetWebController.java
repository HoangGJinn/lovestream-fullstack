package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.response.WatchRoomStateResponse;
import com.hcmute.lovestream.service.watchtogether.WatchTogetherService;
import lombok.RequiredArgsConstructor;
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

        model.addAttribute("videoId", id);
        model.addAttribute("roomCode", roomCode);
        return "videocontent/watch_movie";
    }

}
