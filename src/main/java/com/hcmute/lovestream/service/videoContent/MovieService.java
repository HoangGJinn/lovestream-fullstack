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
        return getMoviesForListing("default", null);
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getMoviesForListing(String sortKey, String userEmail) {
        return getMoviesForListing(sortKey, userEmail, null);
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getMoviesForListing(String sortKey, String userEmail, String keyword) {
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

            return movies.stream()
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



    private Optional<String> resolveUserId(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(userEmail).map(user -> user.getId());
    }

    private RatingStatsBundle buildRatingStats(Collection<String> movieIds) {
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

    private Map<String, Long> buildFavoriteCountMap(Collection<String> movieIds) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : favoriteListRepository.countFavoritesByVideoIds(movieIds)) {
            String movieId = (String) row[0];
            Long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            map.put(movieId, count);
        }
        return map;
    }



    private String safeTitle(String title) {
        return title == null ? "" : title.toLowerCase(Locale.ROOT);
    }

    private record RatingStatsBundle(Map<String, Double> averageRatings, Map<String, Long> ratingCounts) {
    }
}
