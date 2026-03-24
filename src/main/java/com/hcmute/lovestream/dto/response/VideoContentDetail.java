package com.hcmute.lovestream.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoContentDetail {
    private String id;
    private String title;
    private String description;

    private List<String> genres;
    private int releaseYear;
    private String country;
    private int duration;

    private List<String> actors;
    private String director;

    private String posterUrl;
    private String trailerUrl;

    private long views;
    private double rating;

    private boolean canWatch;
    private String watchAction;
}

