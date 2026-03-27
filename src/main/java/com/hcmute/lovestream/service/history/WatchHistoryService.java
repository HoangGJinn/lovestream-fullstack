package com.hcmute.lovestream.service.history;

import com.hcmute.lovestream.dto.request.WatchHistoryProgressRequest;
import com.hcmute.lovestream.dto.response.WatchHistoryItemResponse;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.WatchHistory;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.repository.VideoContentRepository;
import com.hcmute.lovestream.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchHistoryService {

    private static final double COMPLETE_THRESHOLD = 0.95d;

    private final WatchHistoryRepository watchHistoryRepository;
    private final UserRepository userRepository;
    private final VideoContentRepository videoContentRepository;

    @Transactional
    public void recordProgress(String userEmail, WatchHistoryProgressRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Guard: chỉ ghi lịch sử cho phim ACTIVE
        VideoContent videoContent = videoContentRepository
                .findByIdAndStatus(request.getVideoContentId(), ContentStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Nội dung không tồn tại hoặc đã bị ẩn"));

        double progressSeconds = sanitizeNonNegative(request.getCurrentTimeSeconds());
        double durationSeconds = sanitizeDuration(request.getDurationSeconds(), progressSeconds);

        WatchHistory history = watchHistoryRepository
                .findByUserIdAndVideoContentId(user.getId(), videoContent.getId())
                .orElseGet(WatchHistory::new);

        history.setUser(user);
        history.setVideoContent(videoContent);
        history.setProgressSeconds(progressSeconds);
        history.setDurationSeconds(durationSeconds);
        history.setCompleted(isCompleted(progressSeconds, durationSeconds));
        history.setLastWatchedAt(LocalDateTime.now());

        watchHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<WatchHistoryItemResponse> getHistoryByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        return watchHistoryRepository.findByUserIdOrderByLastWatchedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void removeHistoryItem(String userEmail, String videoContentId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        watchHistoryRepository.deleteByUserIdAndVideoContentId(user.getId(), videoContentId);
    }

    private WatchHistoryItemResponse toResponse(WatchHistory history) {
        int percent = calculateProgressPercent(history.getProgressSeconds(), history.getDurationSeconds());
        VideoContent video = history.getVideoContent();

        // Kiểm tra phim còn ACTIVE không — nếu bị ẩn thì không cho click xem tiếp
        boolean available = video.getStatus() != ContentStatus.HIDDEN;
        String watchUrl = available ? ("/watch-movie?id=" + video.getId()) : null;

        return WatchHistoryItemResponse.builder()
                .videoContentId(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .posterUrl(video.getPosterUrl())
                .progressSeconds(history.getProgressSeconds())
                .durationSeconds(history.getDurationSeconds())
                .progressPercent(percent)
                .completed(history.isCompleted())
                .lastWatchedAt(history.getLastWatchedAt())
                .watchUrl(watchUrl)
                .available(available)
                .build();
    }

    private boolean isCompleted(double progress, double duration) {
        if (duration <= 0) {
            return false;
        }
        return progress / duration >= COMPLETE_THRESHOLD;
    }

    private int calculateProgressPercent(double progress, double duration) {
        if (duration <= 0) {
            return 0;
        }
        int percent = (int) Math.round((progress / duration) * 100.0d);
        return Math.max(0, Math.min(100, percent));
    }

    private double sanitizeNonNegative(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return 0;
        }
        return Math.max(0, value);
    }

    private double sanitizeDuration(Double duration, double progress) {
        double normalizedDuration = sanitizeNonNegative(duration);
        if (normalizedDuration <= 0) {
            return progress;
        }
        return Math.max(normalizedDuration, progress);
    }
}
