package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.repository.MediaAssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MediaAssetService {
    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    public String getVideoUrl(String videoContentId) {
        return mediaAssetRepository.findByVideoContent_Id(videoContentId)
                .stream()
                .filter(a -> a.getAssetType() == AssetType.FULL_VIDEO)
                .findFirst()
                .map(MediaAsset::getAssetUrl)
                .orElseThrow();
    }

}
