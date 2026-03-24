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

import java.util.*;
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

        // 2. Chuẩn hóa Key sắp xếp
        String normalizedSort = Optional.ofNullable(sortKey)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .orElse("default");

        // 3. Xây dựng bộ so sánh và thực hiện sắp xếp (KHÔNG dùng Map trung gian tốn
        // kém)
        Comparator<Movie> comparator = buildComparator(normalizedSort, userEmail);
        movies.sort(comparator);

        // 4. Map sang MovieResponse
        return movies.stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    private MovieResponse mapToMovieResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .imagePosterUrl(movie.getPosterUrl()) // Lấy từ hàm @Transient trong Entity
                .build();
    }

    private Comparator<Movie> buildComparator(String sortKey, String userEmail) {
        return switch (sortKey) {
            case "popularity" -> Comparator
                    .comparingInt(Movie::getTotalRatings) // Dùng luôn field có sẵn
                    .reversed()
                    .thenComparing(movie -> safeTitle(movie.getTitle()));
            case "newest" -> Comparator
                    .comparing(Movie::getReleaseDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(movie -> safeTitle(movie.getTitle()));
            case "top_rated" -> Comparator
                    .comparingDouble((Movie movie) -> movie.getAverageRating() != null ? movie.getAverageRating() : 0.0)
                    .reversed()
                    .thenComparing(Comparator.comparingInt(Movie::getTotalRatings).reversed())
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
            case "recommended", "default" -> buildRecommendedComparator(userEmail);
            default -> buildRecommendedComparator(userEmail);
        };
    }

    private Comparator<Movie> buildRecommendedComparator(String userEmail) {
        Optional<String> userIdOpt = resolveUserId(userEmail);

        if (userIdOpt.isEmpty()) {
            return Comparator
                    .comparingDouble((Movie movie) -> recommendationScore(movie, Map.of(), Map.of()))
                    .reversed()
                    .thenComparing(movie -> safeTitle(movie.getTitle()));
        }

        String userId = userIdOpt.get();

        // Chỉ lấy GenreAffinity và PersonalScores (Liên quan đến cá nhân hóa, bắt buộc gọi DB)
        Map<String, Integer> genreAffinity = buildGenreAffinityByUserId(userId);
        Map<String, Integer> personalScores = buildPersonalScoresByUserId(userId);

        return Comparator
                .comparingDouble((Movie movie) -> recommendationScore(movie, genreAffinity, personalScores))
                .reversed()
                .thenComparing(movie -> safeTitle(movie.getTitle()));
    }

    private double recommendationScore(Movie movie,
            Map<String, Integer> genreAffinity,
            Map<String, Integer> personalScores) {
        // Tính điểm tương đồng thể loại
        int affinityScore = movie.getGenres() == null
                ? 0
                : movie.getGenres().stream()
                        .map(Genre::getName)
                        .mapToInt(name -> genreAffinity.getOrDefault(name, 0))
                        .sum();

        // Lấy điểm user đã chấm cho phim này (nếu có)
        int personalScore = personalScores.getOrDefault(movie.getId(), 0);

        // Lấy dữ liệu rating và độ phổ biến TRỰC TIẾP từ Entity
        double avgRating = movie.getAverageRating() != null ? movie.getAverageRating() : 0.0;
        double popularity = (double) movie.getTotalRatings(); // Proxy cho popularity

        return (affinityScore * 2.2) + (personalScore * 2.0) + (avgRating * 1.2) + (popularity * 0.5);
    }

    // --- Các hàm xử lý User và Genre (Giữ lại vì nó phục vụ cá nhân hóa) ---
    private Map<String, Integer> buildGenreAffinityByUserId(String userId) {
        Map<String, Integer> affinity = new HashMap<>();
        for (String genreName : favoriteListRepository.findFavoriteGenreNamesByUserId(userId)) {
            affinity.merge(genreName, 3, Integer::sum);
        }
        for (String genreName : ratingRepository.findPreferredGenreNamesByUserId(userId)) {
            affinity.merge(genreName, 2, Integer::sum);
        }
        return affinity;
    }

    private Map<String, Integer> buildPersonalScoresByUserId(String userId) {
        Map<String, Integer> scores = new HashMap<>();
        for (Object[] row : ratingRepository.findUserScoresByUserId(userId)) {
            String videoId = (String) row[0];
            Integer score = (Integer) row[1];
            if (videoId != null && score != null) {
                scores.put(videoId, score);
            }
        }
        return scores;
    }

    private Optional<String> resolveUserId(String userEmail) {
        if (userEmail == null || userEmail.isBlank())
            return Optional.empty();
        return userRepository.findByEmail(userEmail).map(user -> user.getId());
    }

    private String safeTitle(String title) {
        return title == null ? "" : title.toLowerCase(Locale.ROOT);
    }
}