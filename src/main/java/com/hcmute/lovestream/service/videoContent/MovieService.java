package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.dto.response.MovieResponse;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.mapper.MovieMapper;
import com.hcmute.lovestream.repository.FavoriteListRepository;
import com.hcmute.lovestream.repository.MovieRepository;
import com.hcmute.lovestream.repository.RatingRepository;
import com.hcmute.lovestream.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)

public class MovieService {
    MovieMapper movieMapper;
    MovieRepository movieRepository;
    RatingRepository ratingRepository;
    FavoriteListRepository favoriteListRepository;
    UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MovieResponse> getAllMovies() {
        return getMoviesForListing("default", null);
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getMoviesForListing(String sortKey, String userEmail) {
        // 1. Lấy toàn bộ danh sách Movie (Dữ liệu Rating đã có sẵn trong bảng
        // video_content)
        List<Movie> movies = new ArrayList<>(movieRepository.findAll());

        if (movies.isEmpty()) {
            return List.of();
        }

        List<String> movieIds = movies.stream().map(Movie::getId).toList();
        Map<String, Double> averageRatings = buildAverageRatingMap(movieIds);
        Map<String, Long> ratingCounts = buildRatingCountMap(movieIds);
        Map<String, Long> favoriteCounts = buildFavoriteCountMap(movieIds);

        String normalizedSort = Optional.ofNullable(sortKey)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .orElse("default");

        Comparator<Movie> comparator = buildComparator(normalizedSort, userEmail, averageRatings, ratingCounts, favoriteCounts);
        movies.sort(comparator);

        return movies.stream()
                .map(movieMapper::toMovieResponse)
                .collect(Collectors.toList());
    }

    private Comparator<Movie> buildComparator(String sortKey,
                                              String userEmail,
                                              Map<String, Double> averageRatings,
                                              Map<String, Long> ratingCounts,
                                              Map<String, Long> favoriteCounts) {
        return switch (sortKey) {
            case "popularity" -> Comparator
                    .comparingDouble((Movie movie) -> popularityScore(movie.getId(), ratingCounts, favoriteCounts))
                    .reversed()
                    .thenComparing(movie -> safeTitle(movie.getTitle()));
            case "newest" -> Comparator
                    .comparing(Movie::getReleaseDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(movie -> safeTitle(movie.getTitle()));
            case "top_rated" -> Comparator
                    .comparingDouble((Movie movie) -> averageRatings.getOrDefault(movie.getId(), 0.0))
                    .reversed()
                    .thenComparing(Comparator.comparingLong((Movie movie) -> ratingCounts.getOrDefault(movie.getId(), 0L)).reversed())
                    .thenComparing(movie -> safeTitle(movie.getTitle()));
            case "az" -> Comparator.comparing(movie -> safeTitle(movie.getTitle()));
            case "za" -> Comparator.comparing((Movie movie) -> safeTitle(movie.getTitle())).reversed();
            case "duration_asc" -> Comparator
                    .comparingInt(Movie::getDurationMinutes)
                    .thenComparing(movie -> safeTitle(movie.getTitle()));
            case "duration_desc" -> Comparator
                    .comparingInt(Movie::getDurationMinutes)
                    .reversed()
                    .thenComparing(movie -> safeTitle(movie.getTitle()));
            case "recommended", "default" -> buildRecommendedComparator(userEmail, averageRatings, ratingCounts, favoriteCounts);
            default -> buildRecommendedComparator(userEmail, averageRatings, ratingCounts, favoriteCounts);
        };
    }

    private Comparator<Movie> buildRecommendedComparator(String userEmail,
                                                         Map<String, Double> averageRatings,
                                                         Map<String, Long> ratingCounts,
                                                         Map<String, Long> favoriteCounts) {
        Map<String, Integer> genreAffinity = buildGenreAffinityByEmail(userEmail);
        Map<String, Integer> personalScores = buildPersonalScoresByEmail(userEmail);

        return Comparator
                .comparingDouble((Movie movie) -> recommendationScore(movie, genreAffinity, personalScores, averageRatings, ratingCounts, favoriteCounts))
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

        // Lấy điểm user đã chấm cho phim này (nếu có)
        int personalScore = personalScores.getOrDefault(movie.getId(), 0);
        double avgRating = averageRatings.getOrDefault(movie.getId(), 0.0);
        double popularity = popularityScore(movie.getId(), ratingCounts, favoriteCounts);

        return (affinityScore * 2.2) + (personalScore * 2.0) + (avgRating * 1.2) + popularity;
    }

    private Map<String, Integer> buildGenreAffinityByEmail(String userEmail) {
        Optional<String> userId = resolveUserId(userEmail);
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

    private Map<String, Integer> buildPersonalScoresByEmail(String userEmail) {
        Optional<String> userId = resolveUserId(userEmail);
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

    private Optional<String> resolveUserId(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(userEmail).map(user -> user.getId());
    }

    private Map<String, Double> buildAverageRatingMap(Collection<String> movieIds) {
        Map<String, Double> map = new HashMap<>();
        for (Object[] row : ratingRepository.findRatingStatsByVideoIds(movieIds)) {
            String movieId = (String) row[0];
            Double average = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            map.put(movieId, average);
        }
        return map;
    }

    private Map<String, Long> buildRatingCountMap(Collection<String> movieIds) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : ratingRepository.findRatingStatsByVideoIds(movieIds)) {
            String movieId = (String) row[0];
            Long count = row[2] == null ? 0L : ((Number) row[2]).longValue();
            map.put(movieId, count);
        }
        return map;
    }

    private Map<String, Long> buildFavoriteCountMap(Collection<String> movieIds) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : favoriteListRepository.countFavoritesByVideoIds(movieIds)) {
            String movieId = (String) row[0];
            Long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            map.put(movieId, count);
        }
        return map;
    }

    private double popularityScore(String movieId, Map<String, Long> ratingCounts, Map<String, Long> favoriteCounts) {
        long ratingCount = ratingCounts.getOrDefault(movieId, 0L);
        long favoriteCount = favoriteCounts.getOrDefault(movieId, 0L);
        return (favoriteCount * 2.0) + ratingCount;
    }

    private String safeTitle(String title) {
        return title == null ? "" : title.toLowerCase(Locale.ROOT);
    }
}
