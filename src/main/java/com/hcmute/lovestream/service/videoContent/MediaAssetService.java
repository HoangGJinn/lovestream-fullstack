package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.repository.MediaAssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class MediaAssetService {
    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Cacheable("videoUrl")
    public String getVideoUrl(String videoContentId) {
        return mediaAssetRepository.findVideoUrl(videoContentId)
                .orElseThrow();
    }

}
