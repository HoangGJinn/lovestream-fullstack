package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.repository.MediaAssetRepository;
import com.hcmute.lovestream.repository.VideoContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MediaAssetService {

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Autowired
    private VideoContentRepository videoContentRepository;


    public String getVideoUrl(String videoContentId) {
        videoContentRepository.findByIdAndStatus(videoContentId, ContentStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException(
                        "Nội dung không tồn tại hoặc đã bị ẩn"));

        return mediaAssetRepository.findVideoUrl(videoContentId)
                .orElseThrow(() -> new RuntimeException("Phim chưa có video"));
    }
}
