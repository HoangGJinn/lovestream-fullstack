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

        private static final int HOME_CATEGORY_LIMIT = 24;

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

        List<VideoContent> actionMovies = videoContentRepository
                .findDistinctByStatusAndGenres_Name(ContentStatus.ACTIVE, "Action")
                .stream()
                .limit(HOME_CATEGORY_LIMIT)
                .toList();
        model.addAttribute("actionMovies", actionMovies);

        List<VideoContent> dramaMovies = videoContentRepository
                .findDistinctByStatusAndGenres_Name(ContentStatus.ACTIVE, "Drama")
                .stream()
                .limit(HOME_CATEGORY_LIMIT)
                .toList();
        model.addAttribute("dramaMovies", dramaMovies);

        List<VideoContent> comedyMovies = videoContentRepository
                .findDistinctByStatusAndGenres_Name(ContentStatus.ACTIVE, "Comedy")
                .stream()
                .limit(HOME_CATEGORY_LIMIT)
                .toList();
        model.addAttribute("comedyMovies", comedyMovies);

        List<VideoContent> sciFiMovies = videoContentRepository
                .findDistinctByStatusAndGenres_Name(ContentStatus.ACTIVE, "Science-Fiction")
                .stream()
                .limit(HOME_CATEGORY_LIMIT)
                .toList();
        model.addAttribute("sciFiMovies", sciFiMovies);

        List<VideoContent> featuredPool = java.util.stream.Stream
                .of(actionMovies, dramaMovies, comedyMovies, sciFiMovies)
                .flatMap(List::stream)
                .distinct()
                .toList();

        VideoContent featuredMovie = resolveConfiguredFeaturedMovie()
                .orElseGet(() -> featuredPool.isEmpty()
                        ? null
                        : featuredPool.get(ThreadLocalRandom.current().nextInt(featuredPool.size())));
        model.addAttribute("featuredMovie", featuredMovie);

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
