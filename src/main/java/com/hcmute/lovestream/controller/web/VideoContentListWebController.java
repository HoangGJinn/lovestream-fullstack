package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.response.MovieResponse;
import com.hcmute.lovestream.service.videoContent.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class VideoContentListWebController {

    @Autowired
    private MovieService movieService;

    @GetMapping("/movies")
    public String getMovies(Model model) {
        List<MovieResponse> movies = movieService.getAllMovies();

        List<List<MovieResponse>> rows = new ArrayList<>();
        int size = 6;

        for (int i = 0; i < movies.size(); i += size) {
            rows.add(movies.subList(i, Math.min(i + size, movies.size())));
        }

        model.addAttribute("movieRows", rows);
        return "videocontent/movie/movie_list";
    }
}
