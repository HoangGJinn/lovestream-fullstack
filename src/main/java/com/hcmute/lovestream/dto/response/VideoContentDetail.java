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
    private String slug;        // SEO-friendly URL slug (e.g. "avengers-endgame")
    private String title;
    private String description;

    private List<String> genres;
    private int releaseYear;
    private String ageRating;
    private String quality;
    private String country;
    private int duration;

    /** Legacy fields (kept for existing Thymeleaf bindings). */
    private List<String> actors;
    private String director;

    /** Structured credits for modern UI. */
    private List<CastItem> cast;
    private List<DirectorItem> directors;

    private String posterUrl;
    private String trailerUrl;

    private long views;
    private double rating;

    private boolean canWatch;
    private String watchAction;
    private boolean isFavorited;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CastItem {
        private String personId;
        private String fullName;
        private String characterName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DirectorItem {
        private String personId;
        private String fullName;
    }
}
