package com.hcmute.lovestream.service.share;

import com.hcmute.lovestream.dto.request.ShareRequest;
import com.hcmute.lovestream.dto.response.ShareResponse;
import com.hcmute.lovestream.entity.ShareHistory;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.repository.ShareHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public abstract class AbstractShareContentTemplate {

    protected final ShareHistoryRepository shareHistoryRepository;
    
    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    public AbstractShareContentTemplate(ShareHistoryRepository shareHistoryRepository) {
        this.shareHistoryRepository = shareHistoryRepository;
    }

    /**
     * Template Method xương sống cho việc chia sẻ phim
     */
    @Transactional
    public ShareResponse processShare(User user, VideoContent videoContent, ShareRequest request) {
        // Bước 1: Validate chung
        validateShare(user, videoContent);

        // Bước 2: Tạo link chia sẻ
        String shareLink = generateShareLink(videoContent);

        // Bước 3: Thực thi chia sẻ cụ thể theo từng nền tảng
        boolean isShared = executeShare(user, videoContent, shareLink, request);

        // Bước 4: Xử lý sau khi chia sẻ thành công
        if (isShared) {
            logShareHistory(user, videoContent, request);
            rewardUser(user);
        }

        return buildResponse(isShared, shareLink);
    }

    protected void validateShare(User user, VideoContent videoContent) {
        if (videoContent.getStatus() != ContentStatus.ACTIVE) {
            throw new RuntimeException("Phim này hiện không có sẵn để chia sẻ.");
        }
        if (!user.isActive()) {
            throw new RuntimeException("Tài khoản của bạn chưa được kích hoạt để chia sẻ.");
        }
    }

    protected String generateShareLink(VideoContent videoContent) {
        return appUrl + "/watch/" + videoContent.getSlugOrId();
    }

    /**
     * Ghi nhận lịch sử chia sẻ vào Database
     */
    protected void logShareHistory(User user, VideoContent videoContent, ShareRequest request) {
        try {
            ShareHistory shareHistory = ShareHistory.builder()
                    .user(user)
                    .videoContent(videoContent)
                    .platform(request.getPlatform())
                    .build();
            shareHistoryRepository.save(shareHistory);
            log.info("Đã lưu lịch sử chia sẻ cho user {} với phim {}", user.getEmail(), videoContent.getTitle());
        } catch (Exception e) {
            log.error("Lỗi khi lưu lịch sử chia sẻ: {}", e.getMessage());
        }
    }

    /**
     * Thưởng người dùng (Có thể override hoặc để trống tùy business sau này)
     */
    protected void rewardUser(User user) {
        // Tương lai có thể gọi UserService hoặc PointService để cộng điểm thưởng
        log.info("Chưa có logic cộng điểm cho user {}", user.getEmail());
    }

    protected ShareResponse buildResponse(boolean isShared, String shareLink) {
        if (isShared) {
            return ShareResponse.builder()
                    .success(true)
                    .message("Chia sẻ thành công!")
                    .shareLink(shareLink)
                    .build();
        } else {
            return ShareResponse.builder()
                    .success(false)
                    .message("Chia sẻ thất bại.")
                    .shareLink(shareLink)
                    .build();
        }
    }

    /**
     * Hook abstract do các subclass tự thực hiện
     * @return true nếu chia sẻ thành công, false nếu thất bại
     */
    protected abstract boolean executeShare(User user, VideoContent videoContent, String shareLink, ShareRequest request);
}
