package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.dto.request.VideoContentSearchRequest;
import com.hcmute.lovestream.dto.response.VideoContentSearchResponse;
import com.hcmute.lovestream.entity.VideoContent;

import java.util.Optional;

public interface VideoContentSearchService {

    VideoContentSearchResponse searchVideoContents(VideoContentSearchRequest request);

    Optional<VideoContent> getPublicVideoById(String id);
}

