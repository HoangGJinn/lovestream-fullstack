package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.RatingRequest;
import com.hcmute.lovestream.entity.Rating;
import com.hcmute.lovestream.repository.RatingRepository;
import com.hcmute.lovestream.service.rating.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingRestController {
    private final RatingService ratingService;
    private final RatingRepository ratingRepository;

    // LẤY DANH SÁCH ĐÁNH GIÁ THEO PHIM
    @GetMapping
    public ResponseEntity<?> getRatingsByVideo(@RequestParam String videoContentId) {
        List<Rating> ratings = ratingRepository
                .findByVideoContent_IdOrderByCreatedAtDesc(videoContentId);

        Double avg = ratingRepository.calculateAverageScoreByVideoId(videoContentId);
        int count = ratingRepository.countByVideoContentId(videoContentId);

        List<Map<String, Object>> items = ratings.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("score", r.getScore());
            map.put("review", r.getReview());
            map.put("userName", r.getUser() != null ? r.getUser().getFullName() : "Ẩn danh");
            map.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            return map;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("averageScore", avg != null ? avg : 0.0);
        result.put("totalCount", count);
        result.put("ratings", items);

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<String> rateMovie(
            Principal principal,
            @Valid @RequestBody RatingRequest request) {

        String email = principal.getName();
        String resultMessage = ratingService.processRating(email, request);

        return ResponseEntity.ok(resultMessage);
    }
}
