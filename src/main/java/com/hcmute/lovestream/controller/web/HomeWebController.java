package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.VideoContent;
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

    @GetMapping({"/", "/home"})
    public String homePage(Authentication authentication, Model model) {

        // Phim theo thể loại
        List<VideoContent> actionMovies = videoContentRepository.findByGenres_Name("Action");
        model.addAttribute("actionMovies", actionMovies);

        List<VideoContent> dramaMovies = videoContentRepository.findByGenres_Name("Drama");
        model.addAttribute("dramaMovies", dramaMovies);

        List<VideoContent> comedyMovies = videoContentRepository.findByGenres_Name("Comedy");
        model.addAttribute("comedyMovies", comedyMovies);

        List<VideoContent> sciFiMovies = videoContentRepository.findByGenres_Name("Science-Fiction");
        model.addAttribute("sciFiMovies", sciFiMovies);

        if (authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken)) {
            try {
                model.addAttribute("currentUser",
                        userProfileService.getCurrentUserByEmail(authentication.getName()));
            } catch (RuntimeException ignored) {
            }
        }

        return "home";
    }
}