package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.entity.Episode;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpisodeMediaAssetService {

    private final EpisodeRepository episodeRepository;

    @Transactional(readOnly = true)
    public String getEpisodeVideoUrl(String episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new RuntimeException("Kh\u00f4ng t\u00ecm th\u1ea5y t\u1eadp phim v\u1edbi ID: " + episodeId));

        return episode.getMediaAssets().stream()
                .filter(a -> a != null && a.getAssetType() == AssetType.EPISODE_VIDEO)
                .map(MediaAsset::getAssetUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("T\u1eadp phim n\u00e0y ch\u01b0a c\u00f3 video"));
    }
}
