package com.hcmute.lovestream.dto.request;

import lombok.Data;

@Data
public class VideoContentSearchRequest {

    private String keyword;
    private String genre;
    private Integer year;
    private String country;
    private String type;
    private Integer season;
    private Integer page = 0;
    private Integer size = 24;
}

