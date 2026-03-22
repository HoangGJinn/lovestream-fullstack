package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.RatingRequest;
import com.hcmute.lovestream.service.rating.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;
    @PostMapping
    public ResponseEntity<String> rateMovie(
            Principal principal, // Lấy trực tiếp thông tin người đang đăng nhập
            @Valid @RequestBody RatingRequest request) {

        // Từ chuỗi Token, lấy ra Email của user
        String email = principal.getName();

        // Truyền trực tiếp Email xuống Service xử lý
        String resultMessage = ratingService.processRating(email, request);

        return ResponseEntity.ok(resultMessage);
    }
}
