package com.hcmute.lovestream.service.videoContent.strategies;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.service.videoContent.MovieSortStrategy;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class PopularitySortStrategy implements MovieSortStrategy {

    @Override
    public boolean supports(String sortKey) {
        return "popularity".equals(sortKey);
    }

    @Override
    public Comparator<Movie> getComparator(Map<String, Double> averageRatings, Map<String, Long> ratingCounts, Map<String, Long> favoriteCounts, Optional<String> userId) {
        return Comparator
                .comparingDouble((Movie movie) -> popularityScore(movie.getId(), ratingCounts, favoriteCounts))
                .reversed()
                .thenComparing(movie -> safeTitle(movie.getTitle()));
    }
}
