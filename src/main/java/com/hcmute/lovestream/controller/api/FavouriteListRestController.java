package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.response.FavoriteMovieResponse;
import com.hcmute.lovestream.service.favouritelist.FavoriteListService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavouriteListRestController {
    private final FavoriteListService favoriteListService;

    @GetMapping
    public ResponseEntity<?> getMyFavorites(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String genre) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập!"));
        }
        try {
            String email = authentication.getName();
            List<FavoriteMovieResponse> result = favoriteListService.getMyFavorites(email, keyword, year, genre);
            
            if (result.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "status", "empty",
                        "message", "Danh sách yêu thích của bạn đang trống. Hãy khám phá phim ngay!",
                        "data", result
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", result
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/{videoId}")
    public ResponseEntity<?> addFavorite(
            @PathVariable String videoId,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Bạn cần đăng nhập để thao tác."
            ));
        }

        String email = authentication.getName();
        try {
            boolean isAdded = favoriteListService.addFavoriteMovie(email, videoId);

            String message = isAdded 
                    ? "Đã thêm phim vào danh sách yêu thích!"
                    : "Đã xóa phim khỏi danh sách yêu thích!";

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", message,
                    "isAdded", isAdded
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Lỗi kết nối, vui lòng thử lại."
            ));
        }
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<?> removeFavorite(
            @PathVariable String videoId,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập!"));
        }

        try {
            String email = authentication.getName();
            favoriteListService.removeFavoriteMovie(email, videoId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Đã gỡ phim khỏi danh sách yêu thích!"
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
