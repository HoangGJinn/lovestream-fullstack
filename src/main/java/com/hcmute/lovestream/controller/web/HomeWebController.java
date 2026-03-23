package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.ContentStatus;
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
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            model.addAttribute("currentUser", userProfileService.getCurrentUserByEmail(authentication.getName()));
        }

        // 1. Phim Hành động (Tên tiếng Anh chuẩn từ TVmaze là "Action")
        List<VideoContent> actionMovies = videoContentRepository.findByGenres_Name("Action");
        model.addAttribute("actionMovies", actionMovies);

        // 2. Phim Tâm lý / Tình cảm (Drama)
        List<VideoContent> dramaMovies = videoContentRepository.findByGenres_Name("Drama");
        model.addAttribute("dramaMovies", dramaMovies);

        // 3. Phim Hài hước (Comedy)
        List<VideoContent> comedyMovies = videoContentRepository.findByGenres_Name("Comedy");
        model.addAttribute("comedyMovies", comedyMovies);

        // 4. Viễn tưởng / Kỳ ảo (Science-Fiction hoặc Fantasy)
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