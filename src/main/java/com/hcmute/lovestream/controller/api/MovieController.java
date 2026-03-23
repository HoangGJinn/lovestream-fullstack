package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.response.MovieDetail;
import com.hcmute.lovestream.service.videoContent.MovieDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieDetailService movieDetailService;

    @GetMapping("/{movieId}")
    public ResponseEntity<?> getMovieDetail(@PathVariable String movieId, Authentication authentication) {
        try {
            String userEmail = null;
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                userEmail = authentication.getName();
            }

            MovieDetail dto = movieDetailService.getMovieDetail(movieId, userEmail);
            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ex.getMessage());

        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Không thể tải dữ liệu, vui lòng thử lại");
        }
    }
}

