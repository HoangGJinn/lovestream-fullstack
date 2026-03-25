package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.response.MovieResponse;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.service.videoContent.MovieService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/video-contents")
@lombok.RequiredArgsConstructor
public class VideoContentListController {

    private final MovieService movieService;

    @GetMapping("/movies")
    public List<MovieResponse> getAllMovies() {
        return movieService.getAllMovies();
    }

}
