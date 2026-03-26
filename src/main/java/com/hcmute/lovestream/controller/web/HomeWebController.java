package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.WebContentBanner;
import com.hcmute.lovestream.repository.WebContentBannerRepository;
import com.hcmute.lovestream.repository.VideoContentRepository;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeWebController {

    private final VideoContentRepository videoContentRepository;
    private final UserProfileService userProfileService;
    private final WebContentBannerRepository webContentBannerRepository;

    @GetMapping({ "/", "/home" })
    public String homePage(Authentication authentication, Model model) {
        // Kiểm tra xem người dùng có đang đăng nhập không
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            try {
                model.addAttribute("currentUser",
                        userProfileService.getCurrentUserByEmail(authentication.getName()));
            } catch (RuntimeException ignored) {
            }
        }

        List<WebContentBanner> displayedBanners = webContentBannerRepository.findByIsDisplayedTrueOrderByDisplayOrderAsc();
        model.addAttribute("displayedBanners", displayedBanners);
        model.addAttribute("hasDisplayedBanners", !displayedBanners.isEmpty());

        // 1. Phim Hành động
        List<VideoContent> actionMovies = videoContentRepository.findByGenres_Name("Action").stream()
                .filter(v -> v.getStatus() == ContentStatus.ACTIVE).toList();
        model.addAttribute("actionMovies", actionMovies);

        // 2. Phim Tâm lý / Tình cảm (Drama)
        List<VideoContent> dramaMovies = videoContentRepository.findByGenres_Name("Drama").stream()
                .filter(v -> v.getStatus() == ContentStatus.ACTIVE).toList();
        model.addAttribute("dramaMovies", dramaMovies);

        // 3. Phim Hài hước (Comedy)
        List<VideoContent> comedyMovies = videoContentRepository.findByGenres_Name("Comedy").stream()
                .filter(v -> v.getStatus() == ContentStatus.ACTIVE).toList();
        model.addAttribute("comedyMovies", comedyMovies);

        // 4. Viễn tưởng / Kỳ ảo (Science-Fiction)
        List<VideoContent> sciFiMovies = videoContentRepository.findByGenres_Name("Science-Fiction").stream()
                .filter(v -> v.getStatus() == ContentStatus.ACTIVE).toList();
        model.addAttribute("sciFiMovies", sciFiMovies);

        return "home";
    }
}