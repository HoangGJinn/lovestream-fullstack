package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.entity.ContentCredit;
import com.hcmute.lovestream.entity.Episode;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Season;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.CreditType;
import com.hcmute.lovestream.repository.SeasonRepository;
import com.hcmute.lovestream.repository.TVSeriesRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SeriesDetailService {

    private final TVSeriesRepository tvSeriesRepository;
    private final SeasonRepository seasonRepository;

    @Transactional(readOnly = true)
    public SeriesDetailDto getSeriesDetail(String seriesId, boolean isAuthenticated, boolean isVip) {
        TVSeries series = tvSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Series kh\u00f4ng t\u1ed3n t\u1ea1i"));

        if (series.getStatus() != ContentStatus.ACTIVE) {
            throw new RuntimeException("Series kh\u00f4ng t\u1ed3n t\u1ea1i ho\u1eb7c \u0111\u00e3 b\u1ecb \u1ea9n");
        }

        String posterUrl = findAssetUrl(series.getMediaAssets(), AssetType.POSTER,
                "https://via.placeholder.com/300x450?text=No+Poster");
        String trailerUrl = findAssetUrl(series.getMediaAssets(), AssetType.TRAILER, null);

        String director = extractCredit(series.getContentCredits(), CreditType.DIRECTOR);
        List<String> cast = extractCastList(series.getContentCredits());

        List<String> genres = series.getGenres() == null ? List.of()
                : series.getGenres().stream()
                        .filter(Objects::nonNull)
                        .map(g -> g.getName())
                        .filter(Objects::nonNull)
                        .toList();

        // Load seasons ordered
        List<Season> seasons = seasonRepository.findByTvSeriesOrderBySeasonNumberAsc(series);

        List<SeasonDto> seasonDtos = seasons.stream().map(season -> {
            List<EpisodeDto> episodes = (season.getEpisodes() == null ? List.<Episode>of() : season.getEpisodes())
                    .stream()
                    .sorted((a, b) -> Integer.compare(a.getEpisodeNumber(), b.getEpisodeNumber()))
                    .map(ep -> EpisodeDto.builder()
                            .id(ep.getId())
                            .episodeNumber(ep.getEpisodeNumber())
                            .title(ep.getTitle())
                            .durationInMinutes(ep.getDurationInMinutes())
                            .build())
                    .toList();

            return SeasonDto.builder()
                    .id(season.getId())
                    .seasonNumber(season.getSeasonNumber())
                    .name(season.getName())
                    .releaseYear(season.getReleaseYear())
                    .episodes(episodes)
                    .build();
        }).toList();

        String watchAction = decideWatchAction(isAuthenticated, isVip);

        return SeriesDetailDto.builder()
                .id(series.getId())
                .title(series.getTitle())
                .description(series.getDescription())
                .releaseYear(series.getReleaseYear())
                .genres(genres)
                .director(director)
                .cast(cast)
                .posterUrl(posterUrl)
                .trailerUrl(trailerUrl)
                .durationMinutes(series.getDurationMinutes())
                .seasons(seasonDtos)
                .watchAction(watchAction)
                .build();
    }

    private String decideWatchAction(boolean isAuthenticated, boolean isVip) {
        if (!isAuthenticated) return "LOGIN_REQUIRED";
        if (!isVip) return "BUY_PACKAGE";
        return "WATCH";
    }

    private String findAssetUrl(List<MediaAsset> assets, AssetType type, String defaultValue) {
        if (assets == null) return defaultValue;
        return assets.stream()
                .filter(a -> a != null && a.getAssetType() == type)
                .map(MediaAsset::getAssetUrl)
                .filter(u -> u != null && !u.isBlank())
                .findFirst().orElse(defaultValue);
    }

    private String extractCredit(List<ContentCredit> credits, CreditType type) {
        if (credits == null) return null;
        return credits.stream()
                .filter(c -> c != null && c.getCreditType() == type && c.getPerson() != null)
                .map(c -> c.getPerson().getFullName())
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private List<String> extractCastList(List<ContentCredit> credits) {
        if (credits == null) return List.of();
        return credits.stream()
                .filter(c -> c != null && c.getCreditType() == CreditType.CAST && c.getPerson() != null)
                .map(c -> c.getPerson().getFullName())
                .filter(Objects::nonNull)
                .distinct().toList();
    }

    // ---- Inner DTOs ----

    @Data @Builder
    public static class SeriesDetailDto {
        private String id;
        private String title;
        private String description;
        private int releaseYear;
        private List<String> genres;
        private String director;
        private List<String> cast;
        private String posterUrl;
        private String trailerUrl;
        private Integer durationMinutes;
        private List<SeasonDto> seasons;
        private String watchAction;
    }

    @Data @Builder
    public static class SeasonDto {
        private String id;
        private int seasonNumber;
        private String name;
        private int releaseYear;
        private List<EpisodeDto> episodes;
    }

    @Data @Builder
    public static class EpisodeDto {
        private String id;
        private int episodeNumber;
        private String title;
        private int durationInMinutes;
    }
}
