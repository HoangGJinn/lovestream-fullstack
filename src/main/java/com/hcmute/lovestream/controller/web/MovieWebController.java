package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.response.MovieDetail;
import com.hcmute.lovestream.service.videoContent.MovieDetailService;
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
public class MovieWebController {

    private final MovieDetailService movieDetailService;

    @GetMapping("/movies/{movieId}")
    public String viewMovieDetail(@PathVariable String movieId, Authentication authentication, Model model) {
        String userEmail = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            userEmail = authentication.getName();
        }

        try {
            MovieDetail detail = movieDetailService.getMovieDetail(movieId, userEmail);
            model.addAttribute("movie", detail);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to load movie detail id={}", movieId, ex);
            model.addAttribute("errorMessage", "Không thể tải dữ liệu, vui lòng thử lại");
        }

        return "movies/detail";
    }
}

