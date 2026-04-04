package com.hcmute.lovestream.service.webcontent;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.WebContentBanner;
import com.hcmute.lovestream.entity.enums.WebContentBannerTargetType;
import com.hcmute.lovestream.entity.enums.WebStaticPageType;
import com.hcmute.lovestream.repository.MovieRepository;
import com.hcmute.lovestream.repository.StaticPageRepository;
import com.hcmute.lovestream.repository.TVSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WebContentBannerNavigationResolver {

    private final MovieRepository movieRepository;
    private final TVSeriesRepository tvSeriesRepository;
    private final StaticPageRepository staticPageRepository;

    public void populateResolvedFields(List<WebContentBanner> banners) {
        if (banners == null || banners.isEmpty()) {
            return;
        }
        banners.forEach(this::populateResolvedFields);
    }

    public void populateResolvedFields(WebContentBanner banner) {
        if (banner == null) {
            return;
        }

        ResolvedBannerFields resolved = resolveBannerFields(banner);
        banner.setResolvedNavigationLink(resolved.navigationLink());
        banner.setResolvedTargetLabel(resolved.targetLabel());
        banner.setResolvedDescription(resolved.description());
    }

    private ResolvedBannerFields resolveBannerFields(WebContentBanner banner) {
        if (banner == null) {
            return new ResolvedBannerFields(null, "Không điều hướng", null);
        }

        WebContentBannerTargetType targetType = banner.getTargetType();
        if (targetType == null) {
            String legacyLink = trimToNull(banner.getNavigationLink());
            String legacyLabel = legacyLink == null ? "Không điều hướng" : "Link cũ: " + legacyLink;
            return new ResolvedBannerFields(legacyLink, legacyLabel, null);
        }

        return switch (targetType) {
            case NONE -> new ResolvedBannerFields(null, "Không điều hướng", null);
            case MOVIE -> resolveMovieFields(banner.getTargetRefId());
            case SERIES -> resolveSeriesFields(banner.getTargetRefId());
            case STATIC_PAGE -> {
                String link = resolveStaticPageLink(banner.getTargetRefId());
                String label = resolveStaticPageLabel(banner.getTargetRefId());
                yield new ResolvedBannerFields(link, label, null);
            }
            case EXTERNAL_URL -> {
                String url = firstNonBlank(banner.getExternalUrl(), banner.getNavigationLink());
                String label = url == null ? "URL tùy chỉnh chưa hợp lệ" : "URL tùy chỉnh: " + url;
                yield new ResolvedBannerFields(url, label, null);
            }
        };
    }

    private ResolvedBannerFields resolveMovieFields(String movieId) {
        String normalizedMovieId = trimToNull(movieId);
        if (normalizedMovieId == null) {
            return new ResolvedBannerFields(null, "Phim chưa được chọn", null);
        }

        return movieRepository.findById(normalizedMovieId)
                .map(movie -> new ResolvedBannerFields(
                        "/movies/" + movie.getSlugOrId(),
                        "Phim: " + movie.getTitle(),
                        trimToNull(movie.getDescription())))
                .orElseGet(() -> new ResolvedBannerFields(null, "Phim không tồn tại", null));
    }

    private ResolvedBannerFields resolveSeriesFields(String seriesId) {
        String normalizedSeriesId = trimToNull(seriesId);
        if (normalizedSeriesId == null) {
            return new ResolvedBannerFields(null, "Series chưa được chọn", null);
        }

        return tvSeriesRepository.findById(normalizedSeriesId)
                .map(series -> new ResolvedBannerFields(
                        "/series/" + series.getId(),
                        "Series: " + series.getTitle(),
                        trimToNull(series.getDescription())))
                .orElseGet(() -> new ResolvedBannerFields(null, "Series không tồn tại", null));
    }

    public String resolveNavigationLink(WebContentBanner banner) {
        if (banner == null) {
            return null;
        }

        WebContentBannerTargetType targetType = banner.getTargetType();
        if (targetType == null) {
            return trimToNull(banner.getNavigationLink());
        }

        return switch (targetType) {
            case NONE -> null;
            case MOVIE -> resolveMovieLink(banner.getTargetRefId());
            case SERIES -> resolveSeriesLink(banner.getTargetRefId());
            case STATIC_PAGE -> resolveStaticPageLink(banner.getTargetRefId());
            case EXTERNAL_URL -> firstNonBlank(banner.getExternalUrl(), banner.getNavigationLink());
        };
    }

    public String resolveTargetLabel(WebContentBanner banner) {
        if (banner == null) {
            return "Không điều hướng";
        }

        WebContentBannerTargetType targetType = banner.getTargetType();
        if (targetType == null) {
            String legacyLink = trimToNull(banner.getNavigationLink());
            return legacyLink == null ? "Không điều hướng" : "Link cũ: " + legacyLink;
        }

        return switch (targetType) {
            case NONE -> "Không điều hướng";
            case MOVIE -> resolveMovieLabel(banner.getTargetRefId());
            case SERIES -> resolveSeriesLabel(banner.getTargetRefId());
            case STATIC_PAGE -> resolveStaticPageLabel(banner.getTargetRefId());
            case EXTERNAL_URL -> {
                String url = firstNonBlank(banner.getExternalUrl(), banner.getNavigationLink());
                yield url == null ? "URL tùy chỉnh chưa hợp lệ" : "URL tùy chỉnh: " + url;
            }
        };
    }

    private String resolveMovieLink(String movieId) {
        String normalizedMovieId = trimToNull(movieId);
        if (normalizedMovieId == null) {
            return null;
        }

        return movieRepository.findById(normalizedMovieId)
                .map(movie -> "/movies/" + movie.getSlugOrId())
                .orElse(null);
    }

    private String resolveSeriesLink(String seriesId) {
        String normalizedSeriesId = trimToNull(seriesId);
        if (normalizedSeriesId == null) {
            return null;
        }

        return tvSeriesRepository.findById(normalizedSeriesId)
                .map(series -> "/series/" + series.getId())
                .orElse(null);
    }

    private String resolveStaticPageLink(String pageTypeValue) {
        WebStaticPageType pageType = parseStaticPageType(pageTypeValue);
        if (pageType == null || !staticPageRepository.existsByPageType(pageType)) {
            return null;
        }

        return switch (pageType) {
            case ABOUT -> "/about";
            case PRIVACY_POLICY -> "/privacy-policy";
            case TERMS -> "/terms";
        };
    }

    private String resolveMovieLabel(String movieId) {
        String normalizedMovieId = trimToNull(movieId);
        if (normalizedMovieId == null) {
            return "Phim chưa được chọn";
        }

        return movieRepository.findById(normalizedMovieId)
                .map(Movie::getTitle)
                .map(title -> "Phim: " + title)
                .orElse("Phim không tồn tại");
    }

    private String resolveSeriesLabel(String seriesId) {
        String normalizedSeriesId = trimToNull(seriesId);
        if (normalizedSeriesId == null) {
            return "Series chưa được chọn";
        }

        return tvSeriesRepository.findById(normalizedSeriesId)
                .map(TVSeries::getTitle)
                .map(title -> "Series: " + title)
                .orElse("Series không tồn tại");
    }

    private String resolveStaticPageLabel(String pageTypeValue) {
        WebStaticPageType pageType = parseStaticPageType(pageTypeValue);
        if (pageType == null) {
            return "Trang tĩnh chưa được chọn";
        }

        String label = switch (pageType) {
            case ABOUT -> "Trang tĩnh: Giới thiệu";
            case PRIVACY_POLICY -> "Trang tĩnh: Chính sách bảo mật";
            case TERMS -> "Trang tĩnh: Điều khoản sử dụng";
        };

        if (!staticPageRepository.existsByPageType(pageType)) {
            return label + " (chưa có nội dung)";
        }

        return label;
    }

    private WebStaticPageType parseStaticPageType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }

        try {
            return WebStaticPageType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String firstNonBlank(String primary, String fallback) {
        String normalizedPrimary = trimToNull(primary);
        return normalizedPrimary != null ? normalizedPrimary : trimToNull(fallback);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ResolvedBannerFields(String navigationLink, String targetLabel, String description) {
    }
}
