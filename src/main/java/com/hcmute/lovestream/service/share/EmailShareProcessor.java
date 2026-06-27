package com.hcmute.lovestream.service.share;

import com.hcmute.lovestream.dto.request.ShareRequest;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.repository.ShareHistoryRepository;
import com.hcmute.lovestream.service.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailShareProcessor extends AbstractShareContentTemplate {

    private final EmailService emailService;

    public EmailShareProcessor(ShareHistoryRepository shareHistoryRepository, EmailService emailService) {
        super(shareHistoryRepository);
        this.emailService = emailService;
    }

    @Override
    protected boolean executeShare(User user, VideoContent videoContent, String shareLink, ShareRequest request) {
        if (request.getRecipientEmail() == null || request.getRecipientEmail().isEmpty()) {
            throw new RuntimeException("Email người nhận không được để trống khi chia sẻ qua Email.");
        }
        try {
            log.info("Bắt đầu gửi email chia sẻ từ {} tới {}", user.getEmail(), request.getRecipientEmail());
            emailService.sendShareEmail(
                    request.getRecipientEmail(),
                    user.getFullName(),
                    videoContent.getTitle(),
                    shareLink
            );
            return true;
        } catch (Exception e) {
            log.error("Lỗi khi gửi email chia sẻ: {}", e.getMessage());
            return false;
        }
    }
}
