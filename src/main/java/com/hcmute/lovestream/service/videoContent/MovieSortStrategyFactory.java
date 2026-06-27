package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.service.videoContent.strategies.*;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public final class MovieSortStrategyFactory {
    private MovieSortStrategyFactory() {}

    public static Comparator<Movie> create(String sortKey,
                                           MovieService movieService,
                                           Optional<String> resolvedUserId,
                                           Map<String, Double> averageRatings,
                                           Map<String, Long> ratingCounts,
                                           Map<String, Long> favoriteCounts) {
        String key = sortKey == null ? "default" : sortKey.trim().toLowerCase();
        MovieSortStrategy strategy;
        return switch (key) {
            case "popularity" -> new PopularitySortStrategy().getComparator(resolvedUserId, averageRatings, ratingCounts, favoriteCounts, movieService);
            case "newest" -> new NewestSortStrategy().getComparator(resolvedUserId, averageRatings, ratingCounts, favoriteCounts, movieService);
            case "top_rated" -> new TopRatedSortStrategy().getComparator(resolvedUserId, averageRatings, ratingCounts, favoriteCounts, movieService);
            case "az" -> new AlphabeticalSortStrategy(true).getComparator(resolvedUserId, averageRatings, ratingCounts, favoriteCounts, movieService);
            case "za" -> new AlphabeticalSortStrategy(false).getComparator(resolvedUserId, averageRatings, ratingCounts, favoriteCounts, movieService);
            case "duration_asc" -> new DurationSortStrategy(true).getComparator(resolvedUserId, averageRatings, ratingCounts, favoriteCounts, movieService);
            case "duration_desc" -> new DurationSortStrategy(false).getComparator(resolvedUserId, averageRatings, ratingCounts, favoriteCounts, movieService);
            case "recommended", "default" -> new RecommendedSortStrategy().getComparator(resolvedUserId, averageRatings, ratingCounts, favoriteCounts, movieService);
            default -> new RecommendedSortStrategy().getComparator(resolvedUserId, averageRatings, ratingCounts, favoriteCounts, movieService);
        };
    }
}
