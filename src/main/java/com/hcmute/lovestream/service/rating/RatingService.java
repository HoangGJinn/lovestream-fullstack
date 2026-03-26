package com.hcmute.lovestream.service.rating;

import com.hcmute.lovestream.dto.request.RatingRequest;
import com.hcmute.lovestream.entity.Rating;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.repository.RatingRepository;
import com.hcmute.lovestream.repository.SubscriptionRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.repository.VideoContentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final VideoContentRepository videoRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public String processRating(String email, RatingRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Kiểm tra gói dịch vụ
        boolean hasActive = subscriptionRepository.existsByUser_IdAndStatusAndEndDateAfter(
                user.getId(), SubscriptionStatus.ACTIVE, LocalDateTime.now());
        if (!hasActive) {
            throw new RuntimeException("Bạn cần mua gói dịch vụ để sử dụng tính năng này. Vui lòng đăng ký gói tại trang Gói dịch vụ.");
        }

        VideoContent video = videoRepository.findById(request.getVideoContentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));

        if (video.getStatus() != ContentStatus.ACTIVE) {
            throw new RuntimeException("Nội dung không tồn tại hoặc đã bị ẩn");
        }

        Optional<Rating> existingRatingOpt = ratingRepository.findByUserIdAndVideoContentId(user.getId(), video.getId());

        String message;

        if (existingRatingOpt.isPresent()) {
            Rating existingRating = existingRatingOpt.get();

            if (existingRating.getScore() == request.getScore()) {
                ratingRepository.delete(existingRating);
                message = "Đã xóa đánh giá";
            } else {
                existingRating.setScore(request.getScore());
                existingRating.setReview(request.getReview());
                ratingRepository.save(existingRating);
                message = "Đã cập nhật đánh giá thành " + request.getScore() + " sao";
            }
        } else {
            Rating newRating = new Rating();
            newRating.setScore(request.getScore());
            newRating.setReview(request.getReview());
            newRating.setUser(user);
            newRating.setVideoContent(video);
            ratingRepository.save(newRating);
            message = "Đánh giá " + request.getScore() + " sao thành công!";
        }
        updateVideoAverageRating(video);
        return message;
    }

    private void updateVideoAverageRating(VideoContent video) {
        Double avg = ratingRepository.calculateAverageScoreByVideoId(video.getId());
        int count = ratingRepository.countByVideoContentId(video.getId());

        video.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        video.setTotalRatings(count);

        videoRepository.save(video);
    }

}
