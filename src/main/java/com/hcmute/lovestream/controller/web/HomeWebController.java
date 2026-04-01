package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.repository.VideoContentRepository;
import com.hcmute.lovestream.service.user.UserProfileService;
import com.hcmute.lovestream.service.webcontent.WebContentBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Controller
@RequiredArgsConstructor
public class HomeWebController {

    private final VideoContentRepository videoContentRepository;
    private final UserProfileService userProfileService;
    private final WebContentBannerService bannerService;

        @Value("${app.home.featured-movie:}")
        private String configuredFeaturedMovie;

    @GetMapping({ "/", "/home" })
    public String homePage(Authentication authentication, Model model) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            try {
                model.addAttribute("currentUser", userProfileService.getCurrentUserByEmail(authentication.getName()));
            } catch (RuntimeException ignored) {
            }
        }

        var displayedBanners = bannerService.getDisplayedForHome();
        model.addAttribute("displayedBanners", displayedBanners);
        model.addAttribute("hasDisplayedBanners", !displayedBanners.isEmpty());

        List<VideoContent> activeContents = videoContentRepository.findAll().stream()
                .filter(v -> v.getStatus() == ContentStatus.ACTIVE)
                .toList();

        VideoContent featuredMovie = resolveConfiguredFeaturedMovie()
                .orElseGet(() -> activeContents.isEmpty()
                        ? null
                        : activeContents.get(ThreadLocalRandom.current().nextInt(activeContents.size())));
        model.addAttribute("featuredMovie", featuredMovie);

        List<VideoContent> actionMovies = videoContentRepository.findByGenres_Name("Action").stream()
                .filter(v -> v.getStatus() == ContentStatus.ACTIVE).toList();
        model.addAttribute("actionMovies", actionMovies);

        List<VideoContent> dramaMovies = videoContentRepository.findByGenres_Name("Drama").stream()
                .filter(v -> v.getStatus() == ContentStatus.ACTIVE).toList();
        model.addAttribute("dramaMovies", dramaMovies);

        List<VideoContent> comedyMovies = videoContentRepository.findByGenres_Name("Comedy").stream()
                .filter(v -> v.getStatus() == ContentStatus.ACTIVE).toList();
        model.addAttribute("comedyMovies", comedyMovies);

        List<VideoContent> sciFiMovies = videoContentRepository.findByGenres_Name("Science-Fiction").stream()
                .filter(v -> v.getStatus() == ContentStatus.ACTIVE).toList();
        model.addAttribute("sciFiMovies", sciFiMovies);

        return "home";
    }

        private java.util.Optional<VideoContent> resolveConfiguredFeaturedMovie() {
                if (configuredFeaturedMovie == null || configuredFeaturedMovie.isBlank()) {
                        return java.util.Optional.empty();
                }

                String configuredValue = configuredFeaturedMovie.trim();

                return videoContentRepository.findByIdAndStatus(configuredValue, ContentStatus.ACTIVE)
                                .or(() -> videoContentRepository.findBySlugAndStatus(configuredValue, ContentStatus.ACTIVE));
        }
}
