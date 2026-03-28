package com.hcmute.lovestream.service.rating;

import com.hcmute.lovestream.dto.request.RatingRequest;
import com.hcmute.lovestream.entity.Rating;
import com.hcmute.lovestream.entity.RatingVote;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.repository.*;
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
    private final RatingVoteRepository ratingVoteRepository;

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

    @Transactional
    public void voteRating(String email, String ratingId, boolean isLike) {
        User user = userRepository.findByEmail(email).orElseThrow();
        boolean hasActive = subscriptionRepository.existsByUser_IdAndStatusAndEndDateAfter(
                user.getId(), SubscriptionStatus.ACTIVE, LocalDateTime.now());
        if (!hasActive) {
            throw new RuntimeException("Bạn cần mua gói dịch vụ để thực hiện tính năng này.");
        }
        Rating rating = ratingRepository.findById(ratingId).orElseThrow();
        var existingVote = ratingVoteRepository.findTop1ByUserAndRating(user, rating);
        if (existingVote.isPresent()) {
            RatingVote vote = existingVote.get();
            if (vote.isLike() == isLike) {
                ratingVoteRepository.delete(vote);
                if (isLike) rating.setLikeCount(Math.max(0, rating.getLikeCount() - 1));
                else rating.setDislikeCount(Math.max(0, rating.getDislikeCount() - 1));
            } else {
                vote.setLike(isLike);
                ratingVoteRepository.save(vote);
                if (isLike) {
                    rating.setLikeCount(rating.getLikeCount() + 1);
                    rating.setDislikeCount(Math.max(0, rating.getDislikeCount() - 1));
                } else {
                    rating.setDislikeCount(rating.getDislikeCount() + 1);
                    rating.setLikeCount(Math.max(0, rating.getLikeCount() - 1));
                }
            }
        } else {
            ratingVoteRepository.save(RatingVote.builder().user(user).rating(rating).isLike(isLike).build());
            if (isLike) rating.setLikeCount(rating.getLikeCount() + 1);
            else rating.setDislikeCount(rating.getDislikeCount() + 1);
        }
        ratingRepository.save(rating);
    }

}
