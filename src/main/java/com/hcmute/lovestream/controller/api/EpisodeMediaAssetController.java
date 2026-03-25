package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.service.videoContent.EpisodeMediaAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class EpisodeMediaAssetController {

    private final EpisodeMediaAssetService episodeMediaAssetService;

    /**
     * GET /api/video/watch/episode/{episodeId}
     * Tr\u1ea3 v\u1ec1 URL video c\u1ee7a t\u1eadp phim (EPISODE_VIDEO asset).
     */
    @GetMapping("/watch/episode/{episodeId}")
    public String getEpisodeVideoUrl(@PathVariable String episodeId) {
        return episodeMediaAssetService.getEpisodeVideoUrl(episodeId);
    }
}
