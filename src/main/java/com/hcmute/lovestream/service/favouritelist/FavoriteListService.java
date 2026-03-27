package com.hcmute.lovestream.service.favouritelist;

import com.hcmute.lovestream.dto.response.FavoriteMovieResponse;
import com.hcmute.lovestream.entity.FavoriteList;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import com.hcmute.lovestream.repository.FavoriteListRepository;
import com.hcmute.lovestream.repository.SubscriptionRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.repository.VideoContentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteListService {
    private final FavoriteListRepository favoriteListRepository;
    private final UserRepository userRepository;
    private final VideoContentRepository videoContentRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public boolean addFavoriteMovie(String email, String videoId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));

        boolean isVip = subscriptionRepository.existsByUser_IdAndStatusAndEndDateAfter(
                user.getId(), SubscriptionStatus.ACTIVE, LocalDateTime.now());


        if (!isVip) {
            throw new RuntimeException("Chỉ tài khoản (VIP) đang có gói dịch vụ hoạt động mới được thêm danh sách phim yêu thích. Vui lòng gia hạn/nâng cấp gói!");
        }

        if (favoriteListRepository.existsByUserIdAndVideoId(user.getId(), videoId)) {
            favoriteListRepository.deleteByUserIdAndVideoId(user.getId(), videoId);
            return false;
        }


        VideoContent video = videoContentRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim."));


        FavoriteList favoriteList = FavoriteList.builder()
                .user(user)
                .video(video)
                .build();
        favoriteListRepository.save(favoriteList);
        return true;
    }

    public List<FavoriteMovieResponse> getMyFavorites(String email, String keyword, Integer year, String genre) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        boolean isVip = subscriptionRepository.existsByUser_IdAndStatusAndEndDateAfter(
                user.getId(), SubscriptionStatus.ACTIVE, LocalDateTime.now());

        if (!isVip) {
            throw new RuntimeException("Chỉ tài khoản (VIP) đang có gói dịch vụ hoạt động mới có quyền truy cập Danh sách yêu thích. Hãy nâng cấp ngay!");
        }
        // ============================================
        // Goi DB lấy danh sách và map sang DTO (như cũ)
        List<FavoriteList> favoriteItems = favoriteListRepository.findFilteredFavorites(user.getId(), keyword, year, genre);

        return favoriteItems.stream().map(item -> {
            VideoContent v = item.getVideo();
            return FavoriteMovieResponse.builder()
                    .videoId(v.getId())
                    .title(v.getTitle())
                    .posterUrl(v.getPosterUrl())
                    .releaseYear(v.getReleaseYear())
                    .rating(v.getAverageRating() != null ? v.getAverageRating() : 0.0)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void removeFavoriteMovie(String email, String videoId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        favoriteListRepository.deleteByUserIdAndVideoId(user.getId(), videoId);
    }

}
