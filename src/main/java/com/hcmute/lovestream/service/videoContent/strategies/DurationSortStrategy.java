package com.hcmute.lovestream.service.videoContent.strategies;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.service.videoContent.MovieService;
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
    public Comparator<Movie> getComparator(Optional<String> resolvedUserId, Map<String, Double> averageRatings, Map<String, Long> ratingCounts, Map<String, Long> favoriteCounts, MovieService movieService) {
        Comparator<Movie> comp = Comparator.comparingInt(Movie::getDurationMinutes).thenComparing(movie -> movieService.safeTitle(movie.getTitle()));
        return asc ? comp : comp.reversed();
    }
}
