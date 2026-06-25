package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.entity.Movie;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public interface MovieSortStrategy {
    boolean supports(String sortKey);

    Comparator<Movie> getComparator(
            Map<String, Double> averageRatings,
            Map<String, Long> ratingCounts,
            Map<String, Long> favoriteCounts,
            Optional<String> userId
    );

    default String safeTitle(String title) {
        return title == null ? "" : title.toLowerCase(Locale.ROOT);
    }

    default double popularityScore(String movieId, Map<String, Long> ratingCounts, Map<String, Long> favoriteCounts) {
        long ratingCount = ratingCounts.getOrDefault(movieId, 0L);
        long favoriteCount = favoriteCounts.getOrDefault(movieId, 0L);
        return (favoriteCount * 2.0) + ratingCount;
    }
}
