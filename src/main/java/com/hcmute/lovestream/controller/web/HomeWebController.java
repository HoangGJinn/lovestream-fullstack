package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.repository.VideoContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeWebController {

    private final VideoContentRepository videoContentRepository;

    @GetMapping({"/", "/home"})
    public String homePage(Model model) {

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

        return "home";
    }
}