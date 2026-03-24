package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.response.VideoContentDetail;
import com.hcmute.lovestream.service.videoContent.VideoContentDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
@Slf4j
public class VideoContentDetailController {

    private final VideoContentDetailService movieDetailService;

    @GetMapping("/movies/{movieId}")
    public String viewMovieDetail(@PathVariable String movieId, Authentication authentication, Model model) {
        String userEmail = null;
        boolean isVip = false;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            userEmail = authentication.getName();
            isVip = extractVip(authentication);
        }

        try {
            VideoContentDetail detail = movieDetailService.getMovieDetail(movieId, userEmail, isVip);
            model.addAttribute("movie", detail);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to load movie detail id={}", movieId, ex);
            model.addAttribute("errorMessage", "Không thể tải dữ liệu, vui lòng thử lại");
        }

        return "movies/detail";
    }

    private boolean extractVip(Authentication authentication) {
        Object details = authentication.getDetails();
        if (!(details instanceof java.util.Map<?, ?> detailMap)) {
            return false;
        }
        return Boolean.TRUE.equals(detailMap.get("isVip"));
    }
}

