package com.hcmute.lovestream.service.videoContent.strategy;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.service.videoContent.MovieSortStrategy;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

@Component
public class ReverseAlphabeticalSortStrategy implements MovieSortStrategy {
    @Override
    public boolean supports(String sortKey) {
        return "za".equalsIgnoreCase(sortKey);
    }

    @Override
    public Comparator<Movie> getComparator(
            Map<String, Double> averageRatings,
            Map<String, Long> ratingCounts,
            Map<String, Long> favoriteCounts,
            Optional<String> userId
    ) {
        return Comparator.comparing((Movie movie) -> safeTitle(movie.getTitle())).reversed();
    }
}
