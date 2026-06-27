package com.hcmute.lovestream.service.videoContent.strategies;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.service.videoContent.MovieSortStrategy;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class NewestSortStrategy implements MovieSortStrategy {

    @Override
    public boolean supports(String sortKey) {
        return "newest".equals(sortKey);
    }

    @Override
    public Comparator<Movie> getComparator(Map<String, Double> averageRatings, Map<String, Long> ratingCounts, Map<String, Long> favoriteCounts, Optional<String> userId) {
        return Comparator
                .comparing(Movie::getReleaseDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(movie -> safeTitle(movie.getTitle()));
    }
}
