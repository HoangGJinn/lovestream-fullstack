package com.hcmute.lovestream.dto.response;

import com.hcmute.lovestream.entity.VideoContent;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class VideoContentSearchItemResponse {

    private final String id;
    private final String title;
    private final int releaseYear;
    private final String country;
    private final String type;
    private final List<String> genres;
    private final String posterUrl;
    private final String description;
    private final double score;

    public VideoContentSearchItemResponse(VideoContent videoContent, double score) {
        this.id = videoContent.getId();
        this.title = videoContent.getTitle();
        this.releaseYear = videoContent.getReleaseYear();
        this.country = videoContent.getCountry();
        this.type = videoContent.getClass().getSimpleName().equals("Movie") ? "movie" : "series";
        this.genres = videoContent.getGenres() == null
                ? List.of()
                : videoContent.getGenres().stream().map(g -> g.getName()).collect(Collectors.toList());
        this.posterUrl = videoContent.getPosterUrl();
        this.description = videoContent.getDescription();
        this.score = score;
    }
}

