package com.hcmute.lovestream.service.rating;

import com.hcmute.lovestream.dto.request.RatingRequest;
import com.hcmute.lovestream.entity.Rating;
import com.hcmute.lovestream.entity.enums.ContentStatus;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.repository.RatingRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.repository.VideoContentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final VideoContentRepository videoRepository;
    private final UserRepository userRepository;

    @Transactional

    public String processRating(String email, RatingRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

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
            }else{
                existingRating.setScore(request.getScore());
                ratingRepository.save(existingRating);
                message = "Đã cập nhật đánh giá thành " + request.getScore() + " sao";
            }
        }else{
            Rating newRating = new Rating();
            newRating.setScore(request.getScore());
            newRating.setUser(user);
            newRating.setVideoContent(video);
            ratingRepository.save(newRating);
            message = "Đánh giá" + request.getScore() + " sao";
        }
        updateVideoAverageRating(video);
        return message;
    }

    // Hàm phụ: Tính toán lại và lưu điểm số mới vào bảng Phim
    private void updateVideoAverageRating(VideoContent video) {
        Double avg = ratingRepository.calculateAverageScoreByVideoId(video.getId());
        int count = ratingRepository.countByVideoContentId(video.getId());

        // Nếu không có ai đánh giá thì AVG sẽ null, gán mặc định là 0.0
        video.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0); // Làm tròn 1 chữ số thập phân
        video.setTotalRatings(count);

        videoRepository.save(video);
    }

}
