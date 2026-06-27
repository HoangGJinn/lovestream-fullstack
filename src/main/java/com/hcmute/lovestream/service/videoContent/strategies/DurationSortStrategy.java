package com.hcmute.lovestream.service.videoContent.strategies;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.service.videoContent.MovieSortStrategy;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class DurationSortStrategy implements MovieSortStrategy {
    private final boolean asc;

    public DurationSortStrategy(boolean asc) {
        this.asc = asc;
    }

    @Override
    public boolean supports(String sortKey) {
        return "duration_asc".equals(sortKey) || "duration_desc".equals(sortKey);
    }

    @Override
    public Comparator<Movie> getComparator(Map<String, Double> averageRatings, Map<String, Long> ratingCounts, Map<String, Long> favoriteCounts, Optional<String> userId) {
        Comparator<Movie> comp = Comparator.comparingInt(Movie::getDurationMinutes).thenComparing(movie -> safeTitle(movie.getTitle()));
        return asc ? comp : comp.reversed();
    }
}
