package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.dto.response.VideoContentDetail;
import com.hcmute.lovestream.entity.ContentCredit;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.CreditType;
import com.hcmute.lovestream.repository.MovieRepository;
import com.hcmute.lovestream.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class VideoContentDetailService {

    MovieRepository movieRepository;
    RatingRepository ratingRepository;

    @Transactional(readOnly = true)
    public VideoContentDetail getMovieDetail(String slugOrId, String userEmail, boolean isVip) {
        // Phát hiện sớm UUID để tránh double-query:
        //   - Nếu input là UUID (36 ký tự dạng xxxxxxxx-xxxx-...) → tìm thẳng bằng id
        //   - Ngược lại → tìm bằng slug
        // Điều này đảm bảo mỗi request chỉ cần đúng 1 query DB.
        final boolean isUuid = slugOrId != null
                && slugOrId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        Optional<Movie> movieOpt = isUuid
                ? movieRepository.findDetailedByIdAndStatus(slugOrId, ContentStatus.ACTIVE)
                : movieRepository.findDetailedBySlugAndStatus(slugOrId, ContentStatus.ACTIVE);

        Movie movie = movieOpt.orElseThrow(() ->
                new IllegalArgumentException("Phim không tồn tại hoặc đã bị gỡ khỏi hệ thống."));

        Double avg = ratingRepository.calculateAverageScoreByVideoId(movie.getId());
        double rating = avg == null ? 0.0 : avg;

        String director = extractDirector(movie.getContentCredits());
        List<String> actors = extractActors(movie.getContentCredits());

        List<String> genres = movie.getGenres() == null
                ? List.of()
                : movie.getGenres().stream()
                .filter(Objects::nonNull)
                .map(g -> g.getName())
                .filter(Objects::nonNull)
                .toList();

        String posterUrl = findAssetUrl(movie.getMediaAssets(), AssetType.POSTER, "https://via.placeholder.com/300x450?text=No+Poster");
        String trailerUrl = findAssetUrl(movie.getMediaAssets(), AssetType.TRAILER, null);

        long views = 0L;

        WatchDecision decision = decideWatch(userEmail, isVip);

        return VideoContentDetail.builder()
                .id(movie.getId())
                .slug(movie.getSlug())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genres(genres)
                .releaseYear(movie.getReleaseYear())
                .country(movie.getCountry())
                .duration(movie.getDurationMinutes())
                .actors(actors)
                .director(director)
                .posterUrl(posterUrl)
                .trailerUrl(trailerUrl)
                .views(views)
                .rating(rating)
                .canWatch(decision.canWatch)
                .watchAction(decision.watchAction)
                .build();
    }

    private String extractDirector(List<ContentCredit> credits) {
        if (credits == null) {
            return null;
        }
        return credits.stream()
                .filter(c -> c != null && c.getCreditType() == CreditType.DIRECTOR)
                .map(c -> c.getPerson() != null ? c.getPerson().getFullName() : null)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<String> extractActors(List<ContentCredit> credits) {
        if (credits == null) {
            return List.of();
        }
        return credits.stream()
                .filter(c -> c != null && c.getCreditType() == CreditType.CAST)
                .map(c -> c.getPerson() != null ? c.getPerson().getFullName() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String findAssetUrl(List<MediaAsset> assets, AssetType type, String defaultValue) {
        if (assets == null) {
            return defaultValue;
        }
        return assets.stream()
                .filter(a -> a != null && a.getAssetType() == type)
                .map(MediaAsset::getAssetUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(defaultValue);
    }

    private WatchDecision decideWatch(String userEmail, boolean isVip) {
        if (userEmail == null || userEmail.isBlank()) {
            return new WatchDecision(false, "LOGIN_REQUIRED");
        }

        if (!isVip) {
            return new WatchDecision(false, "BUY_PACKAGE");
        }

        return new WatchDecision(true, "WATCH");
    }

    private static class WatchDecision {
        final boolean canWatch;
        final String watchAction;

        private WatchDecision(boolean canWatch, String watchAction) {
            this.canWatch = canWatch;
            this.watchAction = watchAction;
        }
    }
}
