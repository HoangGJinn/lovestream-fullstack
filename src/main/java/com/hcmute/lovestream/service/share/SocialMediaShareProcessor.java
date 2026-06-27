package com.hcmute.lovestream.service.share;

import com.hcmute.lovestream.dto.request.ShareRequest;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.repository.ShareHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SocialMediaShareProcessor extends AbstractShareContentTemplate {

    public SocialMediaShareProcessor(ShareHistoryRepository shareHistoryRepository) {
        super(shareHistoryRepository);
    }

    @Override
    protected boolean executeShare(User user, VideoContent videoContent, String shareLink, ShareRequest request) {
        // Đối với Social Media, thực tế việc mở hộp thoại diễn ra ở Frontend.
        // Backend chỉ đóng vai trò ghi nhận lịch sử (log).
        log.info("Người dùng {} chia sẻ phim {} qua {}", user.getEmail(), videoContent.getTitle(), request.getPlatform());
        
        // Return true để Template Method tiếp tục chạy hàm logShareHistory và rewardUser
        return true;
    }
}
