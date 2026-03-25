package com.hcmute.lovestream.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class VideoContentSearchResponse {

    private final long totalResults;
    private final List<VideoContentSearchItemResponse> data;
    private final String message;
}

