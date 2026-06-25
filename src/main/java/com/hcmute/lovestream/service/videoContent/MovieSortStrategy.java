package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.entity.Movie;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public interface MovieSortStrategy {
    Comparator<Movie> getComparator(Optional<String> resolvedUserId,
                                     Map<String, Double> averageRatings,
                                     Map<String, Long> ratingCounts,
                                     Map<String, Long> favoriteCounts,
                                     MovieService movieService);
}
