package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.dto.response.MovieResponse;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.ContentStatus;
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
import com.hcmute.lovestream.service.videoContent.MovieSortStrategy;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class MovieService {
    MovieMapper movieMapper;
    MovieRepository movieRepository;
    RatingRepository ratingRepository;
    FavoriteListRepository favoriteListRepository;
    UserRepository userRepository;
    List<MovieSortStrategy> sortStrategies;

    @Transactional(readOnly = true)
    public List<MovieResponse> getAllMovies() {
        return getMoviesForListing("default", null, null, null);
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getMoviesForListing(String sortKey, String userEmail) {
        return getMoviesForListing(sortKey, userEmail, null, null);
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getMoviesForListing(String sortKey, String userEmail, String keyword) {
        return getMoviesForListing(sortKey, userEmail, keyword, null);
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getMoviesForListing(String sortKey, String userEmail, String keyword, Integer age) {
        try {
            List<Movie> movies = new ArrayList<>(movieRepository.findAllByStatusOrderByTitleAsc(ContentStatus.ACTIVE));

            String normalizedKeyword = Optional.ofNullable(keyword)
                    .map(String::trim)
                    .orElse("");
            if (!normalizedKeyword.isEmpty()) {
                String loweredKeyword = normalizedKeyword.toLowerCase(Locale.ROOT);
                movies = movies.stream()
                        .filter(movie -> safeTitle(movie.getTitle()).contains(loweredKeyword))
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            if (movies.isEmpty()) {
                return List.of();
            }

            List<String> movieIds = movies.stream().map(Movie::getId).toList();
            RatingStatsBundle ratingStats = buildRatingStats(movieIds);
            Map<String, Double> averageRatings = ratingStats.averageRatings();
            Map<String, Long> ratingCounts = ratingStats.ratingCounts();
            Map<String, Long> favoriteCounts = buildFavoriteCountMap(movieIds);

            String normalizedSort = Optional.ofNullable(sortKey)
                    .map(s -> s.trim().toLowerCase(Locale.ROOT))
                    .filter(s -> !s.isEmpty())
                    .orElse("default");

            Optional<String> resolvedUserId = resolveUserId(userEmail);

            MovieSortStrategy strategy = sortStrategies.stream()
                    .filter(s -> s.supports(normalizedSort))
                    .findFirst()
                    .orElseGet(() -> sortStrategies.stream()
                            .filter(s -> s.supports("default"))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Default sort strategy not found")));

            Comparator<Movie> comparator = strategy.getComparator(averageRatings, ratingCounts, favoriteCounts, resolvedUserId);
            movies.sort(comparator);

            List<Movie> filteredMovies = movies;
            if (age != null) {
                List<Movie> temp = new ArrayList<>();
                com.hcmute.lovestream.util.AgeRestrictedIterator<Movie> ageIterator = 
                        new com.hcmute.lovestream.util.AgeRestrictedIterator<>(movies.iterator(), age);
                while (ageIterator.hasNext()) {
                    temp.add(ageIterator.next());
                }
                filteredMovies = temp;
            }

            return filteredMovies.stream()
                    .map(movieMapper::toMovieResponse)
                    .collect(Collectors.toList());
        } catch (org.springframework.dao.DataAccessException e) {
            // lỗi DB
            throw new RuntimeException("Không thể kết nối hệ thống", e);
        }
    }

    @Transactional(readOnly = true)
    public boolean hasAnyActiveMovies() {
        return movieRepository.countByStatus(ContentStatus.ACTIVE) > 0;
    }

    private Comparator<Movie> buildComparator(String sortKey,
                                              Optional<String> resolvedUserId,
                                              Map<String, Double> averageRatings,
                                              Map<String, Long> ratingCounts,
                                              Map<String, Long> favoriteCounts) {
        return MovieSortStrategyFactory.create(sortKey, this, resolvedUserId, averageRatings, ratingCounts, favoriteCounts);
    }

    // dựa trên sở thích thể loại của user, điểm cá nhân user đã chấm cho phim, điểm trung bình, độ phổ biến của phim để tính điểm đề xuất
    public Comparator<Movie> buildRecommendedComparator(Optional<String> resolvedUserId,
                                                        Map<String, Double> averageRatings,
                                                        Map<String, Long> ratingCounts,
                                                        Map<String, Long> favoriteCounts) {
        Map<String, Integer> genreAffinity = buildGenreAffinity(resolvedUserId);
        Map<String, Integer> personalScores = buildPersonalScores(resolvedUserId);

        return Comparator
                .comparingDouble((Movie movie) -> recommendationScore(movie, genreAffinity, personalScores,
                        averageRatings, ratingCounts, favoriteCounts))
                .reversed()
                .thenComparing(movie -> safeTitle(movie.getTitle()));
    }

    public double recommendationScore(Movie movie,
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

    Map<String, Integer> buildGenreAffinity(Optional<String> userId) {
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

    Map<String, Integer> buildPersonalScores(Optional<String> userId) {
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

    Optional<String> resolveUserId(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(userEmail).map(user -> user.getId());
    }

    RatingStatsBundle buildRatingStats(Collection<String> movieIds) {
        Map<String, Double> averageMap = new HashMap<>();
        Map<String, Long> countMap = new HashMap<>();

        for (Object[] row : ratingRepository.findRatingStatsByVideoIds(movieIds)) {
            String movieId = (String) row[0];
            Double average = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            Long count = row[2] == null ? 0L : ((Number) row[2]).longValue();
            averageMap.put(movieId, average);
            countMap.put(movieId, count);
        }

        return new RatingStatsBundle(averageMap, countMap);
    }

    Map<String, Long> buildFavoriteCountMap(Collection<String> movieIds) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : favoriteListRepository.countFavoritesByVideoIds(movieIds)) {
            String movieId = (String) row[0];
            Long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            map.put(movieId, count);
        }
        return map;
    }

    public double popularityScore(String movieId, Map<String, Long> ratingCounts, Map<String, Long> favoriteCounts) {
        long ratingCount = ratingCounts.getOrDefault(movieId, 0L);
        long favoriteCount = favoriteCounts.getOrDefault(movieId, 0L);
        return (favoriteCount * 2.0) + ratingCount;
    }

    public String safeTitle(String title) {
        return title == null ? "" : title.toLowerCase(Locale.ROOT);
    }

    private record RatingStatsBundle(Map<String, Double> averageRatings, Map<String, Long> ratingCounts) {
    }
}