package com.hcmute.lovestream.service.videoContent.strategies;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.service.videoContent.MovieService;
import com.hcmute.lovestream.service.videoContent.MovieSortStrategy;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class TopRatedSortStrategy implements MovieSortStrategy {
    @Override
    public Comparator<Movie> getComparator(Optional<String> resolvedUserId, Map<String, Double> averageRatings, Map<String, Long> ratingCounts, Map<String, Long> favoriteCounts, MovieService movieService) {
        return Comparator
                .comparingDouble((Movie movie) -> averageRatings.getOrDefault(movie.getId(), 0.0))
                .reversed()
                .thenComparing(Comparator
                        .comparingLong((Movie movie) -> ratingCounts.getOrDefault(movie.getId(), 0L)).reversed())
                .thenComparing(movie -> movieService.safeTitle(movie.getTitle()));
    }
}
