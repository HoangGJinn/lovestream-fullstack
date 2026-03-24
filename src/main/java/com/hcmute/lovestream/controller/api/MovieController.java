package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.response.VideoContentDetail;
import com.hcmute.lovestream.service.videoContent.VideoContentDetailService;
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

    private final VideoContentDetailService movieDetailService;

    @GetMapping("/{movieId}")
    public ResponseEntity<?> getMovieDetail(@PathVariable String movieId, Authentication authentication) {
        try {
            String userEmail = null;
            boolean isVip = false;
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                userEmail = authentication.getName();
                isVip = extractVip(authentication);
            }

            VideoContentDetail dto = movieDetailService.getMovieDetail(movieId, userEmail, isVip);
            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ex.getMessage());

        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Không thể tải dữ liệu, vui lòng thử lại");
        }
    }

    private boolean extractVip(Authentication authentication) {
        Object details = authentication.getDetails();
        if (!(details instanceof java.util.Map<?, ?> detailMap)) {
            return false;
        }
        return Boolean.TRUE.equals(detailMap.get("isVip"));
    }
}

