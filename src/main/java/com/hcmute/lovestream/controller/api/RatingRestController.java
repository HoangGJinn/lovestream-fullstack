package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.RatingRequest;
import com.hcmute.lovestream.entity.Rating;
import com.hcmute.lovestream.repository.RatingRepository;
import com.hcmute.lovestream.service.rating.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @GetMapping
    public ResponseEntity<?> getRatingsByVideo(@RequestParam String videoContentId) {
        List<Rating> ratings = ratingRepository.findByVideoContent_IdOrderByCreatedAtDesc(videoContentId);
        Double avg = ratingRepository.calculateAverageScoreByVideoId(videoContentId);
        int count = ratingRepository.countByVideoContentId(videoContentId);

        List<Map<String, Object>> items = ratings.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("score", r.getScore());
            map.put("review", r.getReview());
            map.put("userName", r.getUser() != null ? r.getUser().getFullName() : "Ẩn danh");
            map.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);

            // QUAN TRỌNG: Phải thêm 2 dòng này để Frontend có số hiện lên
            map.put("likeCount", r.getLikeCount());
            map.put("dislikeCount", r.getDislikeCount());

            return map;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("averageScore", avg != null ? avg : 0.0);
        result.put("totalCount", count);
        result.put("ratings", items);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeRating(Principal principal, @PathVariable String id) {
        // Kiểm tra xem đã đăng nhập chưa để tránh lỗi "System Error"
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để thích đánh giá!");
        }
        ratingService.voteRating(principal.getName(), id, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<?> dislikeRating(Principal principal, @PathVariable String id) {


        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để không thích đánh giá!");
        }
        ratingService.voteRating(principal.getName(), id, false);
        return ResponseEntity.ok().build();
    }

    // Giữ nguyên hàm rateMovie cũ của bạn bên dưới...
}
