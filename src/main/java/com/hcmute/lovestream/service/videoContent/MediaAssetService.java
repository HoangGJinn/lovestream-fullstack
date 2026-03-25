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

    /**
     * Trả URL video cho phim. Chỉ cho phép nếu phim ACTIVE.
     * Guard này chặn cả API GET /api/video/watch/{id} và JS player trong watch_movie.html.
     */
    public String getVideoUrl(String videoContentId) {
        // Guard: chặn truy cập nếu phim bị ẩn / không tồn tại
        videoContentRepository.findByIdAndStatus(videoContentId, ContentStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException(
                        "Nội dung không tồn tại hoặc đã bị ẩn"));

        return mediaAssetRepository.findVideoUrl(videoContentId)
                .orElseThrow(() -> new RuntimeException("Phim chưa có video"));
    }
}
