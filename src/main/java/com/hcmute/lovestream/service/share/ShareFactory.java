package com.hcmute.lovestream.service.share;

import com.hcmute.lovestream.entity.enums.SharePlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShareFactory {

    private final EmailShareProcessor emailShareProcessor;
    private final SocialMediaShareProcessor socialMediaShareProcessor;

    public AbstractShareContentTemplate getProcessor(SharePlatform platform) {
        if (platform == SharePlatform.EMAIL) {
            return emailShareProcessor;
        }
        return socialMediaShareProcessor;
    }
}
