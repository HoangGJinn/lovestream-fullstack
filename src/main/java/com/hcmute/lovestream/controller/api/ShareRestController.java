package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.ShareRequest;
import com.hcmute.lovestream.dto.response.ShareResponse;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.repository.VideoContentRepository;
import com.hcmute.lovestream.service.share.AbstractShareContentTemplate;
import com.hcmute.lovestream.service.share.ShareFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
public class ShareRestController {

    private final ShareFactory shareFactory;
    private final UserRepository userRepository;
    private final VideoContentRepository videoContentRepository;

    @PostMapping
    public ResponseEntity<ShareResponse> shareVideoContent(@Valid @RequestBody ShareRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ShareResponse.builder().success(false).message("Vui lòng đăng nhập để chia sẻ.").build());
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(ShareResponse.builder().success(false).message("Không tìm thấy người dùng.").build());
        }

        VideoContent videoContent = videoContentRepository.findById(request.getVideoId()).orElse(null);
        if (videoContent == null) {
            return ResponseEntity.status(404).body(ShareResponse.builder().success(false).message("Không tìm thấy phim.").build());
        }

        AbstractShareContentTemplate processor = shareFactory.getProcessor(request.getPlatform());
        
        try {
            ShareResponse response = processor.processShare(user, videoContent, request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ShareResponse.builder().success(false).message(e.getMessage()).build());
        }
    }
}
