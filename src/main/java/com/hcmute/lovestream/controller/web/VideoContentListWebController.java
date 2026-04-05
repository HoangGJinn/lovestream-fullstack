package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.response.MovieResponse;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.repository.TVSeriesRepository;
import com.hcmute.lovestream.service.videoContent.MovieService;
import com.hcmute.lovestream.service.user.UserProfileService;
import com.hcmute.lovestream.util.VietnameseNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class VideoContentListWebController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private TVSeriesRepository tvSeriesRepository;

    @Autowired
    private UserProfileService userProfileService;

    @GetMapping("/movies")
    public String getMovies(@RequestParam(name = "sort", defaultValue = "default") String sort,
                            @RequestParam(required = false) String keyword,
                            Authentication authentication,
                            Model model) {
        // Normalize keyword so /movies works independently (trim + treat blank as null)
        keyword = (StringUtils.hasText(keyword) ? keyword.trim() : null);

        String userEmail = null;
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            userEmail = authentication.getName();
            try {
                model.addAttribute("currentUser", userProfileService.getCurrentUserByEmail(userEmail));
            } catch (RuntimeException ignored) {
            }
        }

        List<MovieResponse> movies = movieService.getMoviesForListing(sort, userEmail, keyword);

        List<List<MovieResponse>> rows = new ArrayList<>();
        int size = 6;

        for (int i = 0; i < movies.size(); i += size) {
            rows.add(movies.subList(i, Math.min(i + size, movies.size())));
        }

        model.addAttribute("movieRows", rows);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("currentKeyword", keyword);
        return "videocontent/movie/movie_list";
    }

    @GetMapping("/series")
    public String getSeries(@RequestParam(required = false) String keyword,
                            @RequestParam(name = "sort", defaultValue = "default") String sort,
                            Authentication authentication,
                            Model model) {
        keyword = (StringUtils.hasText(keyword) ? keyword.trim() : null);

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            try {
                model.addAttribute("currentUser",
                        userProfileService.getCurrentUserByEmail(authentication.getName()));
            } catch (RuntimeException ignored) {
            }
        }

        List<TVSeries> series = tvSeriesRepository.findAllByStatus(com.hcmute.lovestream.entity.enums.ContentStatus.ACTIVE);
        if (keyword != null) {
            String loweredKeyword = keyword.toLowerCase(Locale.ROOT);
            String normalizedKeyword = VietnameseNormalizer.normalize(keyword);
            series = series.stream()
                    .filter(item -> {
                        String title = item.getTitle() == null ? "" : item.getTitle().toLowerCase(Locale.ROOT);
                        String titleUnsigned = item.getTitleUnsigned() == null ? "" : item.getTitleUnsigned();
                        return title.contains(loweredKeyword) || titleUnsigned.contains(normalizedKeyword);
                    })
                    .toList();
        }

        List<List<TVSeries>> rows = new ArrayList<>();
        int size = 6;

        for (int i = 0; i < series.size(); i += size) {
            rows.add(series.subList(i, Math.min(i + size, series.size())));
        }

        model.addAttribute("seriesRows", rows);
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("selectedSort", sort);
        return "videocontent/series/series_list";
    }
}
