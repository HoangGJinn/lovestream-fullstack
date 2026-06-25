package com.hcmute.lovestream.service.videoContent.strategy;

import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.repository.FavoriteListRepository;
import com.hcmute.lovestream.repository.RatingRepository;
import com.hcmute.lovestream.service.videoContent.MovieSortStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RecommendedSortStrategy implements MovieSortStrategy {

    private final FavoriteListRepository favoriteListRepository;
    private final RatingRepository ratingRepository;

    @Override
    public boolean supports(String sortKey) {
        return "recommended".equalsIgnoreCase(sortKey) || "default".equalsIgnoreCase(sortKey);
    }

    @Override
    public Comparator<Movie> getComparator(
            Map<String, Double> averageRatings,
            Map<String, Long> ratingCounts,
            Map<String, Long> favoriteCounts,
            Optional<String> userId
    ) {
        Map<String, Integer> genreAffinity = buildGenreAffinity(userId);
        Map<String, Integer> personalScores = buildPersonalScores(userId);

        return Comparator
                .comparingDouble((Movie movie) -> recommendationScore(movie, genreAffinity, personalScores,
                        averageRatings, ratingCounts, favoriteCounts))
                .reversed()
                .thenComparing(movie -> safeTitle(movie.getTitle()));
    }

    private double recommendationScore(Movie movie,
            Map<String, Integer> genreAffinity,
            Map<String, Integer> personalScores,
            Map<String, Double> averageRatings,
            Map<String, Long> ratingCounts,
            Map<String, Long> favoriteCounts) {
        int affinityScore = movie.getGenres() == null
                ? 0
                : movie.getGenres().stream()
                        .map(Genre::getName)
                        .mapToInt(name -> genreAffinity.getOrDefault(name, 0))
                        .sum();

        int personalScore = personalScores.getOrDefault(movie.getId(), 0);
        double avgRating = averageRatings.getOrDefault(movie.getId(), 0.0);
        double popularity = popularityScore(movie.getId(), ratingCounts, favoriteCounts);

        return (affinityScore * 2.2) + (personalScore * 2.0) + (avgRating * 1.2) + popularity;
    }

    private Map<String, Integer> buildGenreAffinity(Optional<String> userId) {
        if (userId.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> affinity = new HashMap<>();
        for (String genreName : favoriteListRepository.findFavoriteGenreNamesByUserId(userId.get())) {
            affinity.merge(genreName, 3, Integer::sum);
        }
        for (String genreName : ratingRepository.findPreferredGenreNamesByUserId(userId.get())) {
            affinity.merge(genreName, 2, Integer::sum);
        }
        return affinity;
    }

    private Map<String, Integer> buildPersonalScores(Optional<String> userId) {
        if (userId.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> scores = new HashMap<>();
        for (Object[] row : ratingRepository.findUserScoresByUserId(userId.get())) {
            String videoId = (String) row[0];
            Integer score = (Integer) row[1];
            if (videoId != null && score != null) {
                scores.put(videoId, score);
            }
        }
        return scores;
    }
}
