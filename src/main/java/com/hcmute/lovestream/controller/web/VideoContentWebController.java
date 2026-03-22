package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.request.VideoContentSearchRequest;
import com.hcmute.lovestream.dto.response.VideoContentSearchResponse;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.service.videocontent.VideoContentSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class VideoContentWebController {

    private final VideoContentSearchService videoContentSearchService;
    private final GenreRepository genreRepository;

    @GetMapping("/videocontents")
    public String redirectToSearchPage() {
        return "redirect:/videocontents/search";
    }

    @GetMapping("/videocontents/search/")
    public String redirectTrailingSlashSearch() {
        return "redirect:/videocontents/search";
    }

    @GetMapping("/videocontents/search")
    public String viewSearchPage(Model model,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String genre,
                                 @RequestParam(required = false) Integer year,
                                 @RequestParam(required = false) String country,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) Integer season,
                                 @RequestParam(defaultValue = "0") Integer page,
                                 @RequestParam(defaultValue = "24") Integer size) {

        log.info("Render SearchPage /videocontents/search keyword={}, genre={}, year={}, country={}, type={}, season={}, page={}, size={}",
                keyword, genre, year, country, type, season, page, size);

        VideoContentSearchRequest request = toSearchRequest(keyword, genre, year, country, type, season, page, size);
        VideoContentSearchResponse response = videoContentSearchService.searchVideoContents(request);

        model.addAttribute("searchRequest", request);
        model.addAttribute("searchResponse", response);
        model.addAttribute("genres", genreRepository.findAll());
        return "videocontent/search";
    }

    @GetMapping(value = "/videocontents/search", params = "format=json")
    @ResponseBody
    public ResponseEntity<VideoContentSearchResponse> searchVideoContents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer season,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "24") Integer size) {

        VideoContentSearchRequest request = toSearchRequest(keyword, genre, year, country, type, season, page, size);

        log.info("GET /videocontents/search(format=json) keyword={}, genre={}, year={}, country={}, type={}, season={}, page={}, size={}",
                keyword, genre, year, country, type, season, page, size);

        return ResponseEntity.ok(videoContentSearchService.searchVideoContents(request));
    }

    private VideoContentSearchRequest toSearchRequest(String keyword,
                                                      String genre,
                                                      Integer year,
                                                      String country,
                                                      String type,
                                                      Integer season,
                                                      Integer page,
                                                      Integer size) {
        VideoContentSearchRequest request = new VideoContentSearchRequest();
        request.setKeyword(keyword);
        request.setGenre(genre);
        request.setYear(year);
        request.setCountry(country);
        request.setType(type);
        request.setSeason(season);
        request.setPage(page);
        request.setSize(size);
        return request;
    }

    @GetMapping("/videocontents/{id:[0-9a-fA-F\\-]{36}}")
    public String viewDetail(@PathVariable String id, Model model) {
        Optional<VideoContent> videoOpt = videoContentSearchService.getPublicVideoById(id);
        if (videoOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Noi dung khong ton tai hoac da bi an");
            return "videocontent/detail";
        }

        model.addAttribute("video", videoOpt.get());
        return "videocontent/detail";
    }
}

