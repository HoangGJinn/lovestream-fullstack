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
        return switch (key) {
            case "popularity"   -> new PopularitySortStrategy().getComparator(averageRatings, ratingCounts, favoriteCounts, resolvedUserId);
            case "newest"       -> new NewestSortStrategy().getComparator(averageRatings, ratingCounts, favoriteCounts, resolvedUserId);
            case "top_rated"    -> new TopRatedSortStrategy().getComparator(averageRatings, ratingCounts, favoriteCounts, resolvedUserId);
            case "az"           -> new AlphabeticalSortStrategy(true).getComparator(averageRatings, ratingCounts, favoriteCounts, resolvedUserId);
            case "za"           -> new AlphabeticalSortStrategy(false).getComparator(averageRatings, ratingCounts, favoriteCounts, resolvedUserId);
            case "duration_asc" -> new DurationSortStrategy(true).getComparator(averageRatings, ratingCounts, favoriteCounts, resolvedUserId);
            case "duration_desc"-> new DurationSortStrategy(false).getComparator(averageRatings, ratingCounts, favoriteCounts, resolvedUserId);
            default             -> new RecommendedSortStrategy(movieService).getComparator(averageRatings, ratingCounts, favoriteCounts, resolvedUserId);
        };
    }
}
