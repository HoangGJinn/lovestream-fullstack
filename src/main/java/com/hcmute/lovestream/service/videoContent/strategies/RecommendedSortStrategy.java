package com.hcmute.lovestream.service.videoContent.strategies;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.service.videoContent.MovieService;
import com.hcmute.lovestream.service.videoContent.MovieSortStrategy;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class RecommendedSortStrategy implements MovieSortStrategy {

    private final MovieService movieService;

    public RecommendedSortStrategy(MovieService movieService) {
        this.movieService = movieService;
    }

    @Override
    public boolean supports(String sortKey) {
        return "recommended".equals(sortKey) || "default".equals(sortKey);
    }

    @Override
    public Comparator<Movie> getComparator(Map<String, Double> averageRatings, Map<String, Long> ratingCounts, Map<String, Long> favoriteCounts, Optional<String> userId) {
        return movieService.buildRecommendedComparator(userId, averageRatings, ratingCounts, favoriteCounts);
    }
}
