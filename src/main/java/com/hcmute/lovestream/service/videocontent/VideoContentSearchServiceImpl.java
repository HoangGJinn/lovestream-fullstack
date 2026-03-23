package com.hcmute.lovestream.service.videocontent;

import com.hcmute.lovestream.dto.request.VideoContentSearchRequest;
import com.hcmute.lovestream.dto.response.VideoContentSearchItemResponse;
import com.hcmute.lovestream.dto.response.VideoContentSearchResponse;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.Season;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.repository.VideoContentRepository;
import com.hcmute.lovestream.util.VietnameseNormalizer;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoContentSearchServiceImpl implements VideoContentSearchService {

    private static final int FALLBACK_POOL_SIZE = 250;

    private final VideoContentRepository videoContentRepository;

    @Override
    @Transactional(readOnly = true)
    public VideoContentSearchResponse searchVideoContents(VideoContentSearchRequest request) {
        int page = sanitizePage(request.getPage());
        int size = sanitizeSize(request.getSize());
        String keyword = trimToNull(request.getKeyword());
        String keywordUnsigned = keyword == null ? null : VietnameseNormalizer.normalize(keyword);

        Specification<VideoContent> baseFilterSpec = buildSpecification(request, null, null);
        Specification<VideoContent> strictSearchSpec = buildSpecification(request, keyword, keywordUnsigned);

        Pageable pageable = PageRequest.of(page, size);
        Page<VideoContent> strictPage = videoContentRepository.findAll(strictSearchSpec, pageable);

        List<ScoredVideo> ranked = strictPage.getContent().stream()
                .map(v -> new ScoredVideo(v, computeMatchScore(v, keyword, keywordUnsigned)))
                .sorted(Comparator.comparingDouble(ScoredVideo::score).reversed()
                        .thenComparing((ScoredVideo sv) -> sv.video().getReleaseYear(), Comparator.reverseOrder())
                        .thenComparing(sv -> safe(sv.video().getTitle())))
                .collect(Collectors.toList());

        long totalResults = strictPage.getTotalElements();

        if (keyword != null && ranked.isEmpty()) {
            // Fallback fuzzy: lấy một tập dữ liệu đã lọc theo filter động rồi chấm điểm gần đúng.
            Page<VideoContent> fallbackPage = videoContentRepository.findAll(baseFilterSpec, PageRequest.of(0, FALLBACK_POOL_SIZE));
            ranked = fallbackPage.getContent().stream()
                    .map(v -> new ScoredVideo(v, computeMatchScore(v, keyword, keywordUnsigned)))
                    .filter(scored -> scored.score() >= 0.42d)
                    .sorted(Comparator.comparingDouble(ScoredVideo::score).reversed()
                            .thenComparing((ScoredVideo sv) -> sv.video().getReleaseYear(), Comparator.reverseOrder())
                            .thenComparing(sv -> safe(sv.video().getTitle())))
                    .collect(Collectors.toList());
            totalResults = ranked.size();
        }

        List<VideoContentSearchItemResponse> resultData = paginateRanked(ranked, page, size).stream()
                .map(scored -> new VideoContentSearchItemResponse(scored.video(), scored.score()))
                .collect(Collectors.toList());

        if (resultData.isEmpty()) {
            return VideoContentSearchResponse.builder()
                    .totalResults(0)
                    .data(List.of())
                    .message("Không tìm thấy phim")
                    .build();
        }

        return VideoContentSearchResponse.builder()
                .totalResults(totalResults)
                .data(resultData)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoContent> getPublicVideoById(String id) {
        return videoContentRepository.findById(id)
                .filter(video -> video.getStatus() == ContentStatus.ACTIVE);
    }

    private Specification<VideoContent> buildSpecification(VideoContentSearchRequest request,
                                                           String keyword,
                                                           String keywordUnsigned) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ContentStatus.ACTIVE));

            if (keyword != null && keywordUnsigned != null) {
                String likeSigned = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                String likeUnsigned = "%" + keywordUnsigned + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), likeSigned),
                        cb.like(cb.lower(root.get("titleUnsigned")), likeUnsigned)
                ));
            }

            if (trimToNull(request.getGenre()) != null) {
                Join<Object, Object> genreJoin = root.join("genres", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(genreJoin.get("name")), request.getGenre().trim().toLowerCase(Locale.ROOT)));
            }

            if (request.getYear() != null) {
                predicates.add(cb.equal(root.get("releaseYear"), request.getYear()));
            }

            if (trimToNull(request.getCountry()) != null) {
                predicates.add(cb.like(cb.lower(root.get("country")), "%" + request.getCountry().trim().toLowerCase(Locale.ROOT) + "%"));
            }

            if (trimToNull(request.getType()) != null) {
                String normalizedType = request.getType().trim().toLowerCase(Locale.ROOT);
                if ("movie".equals(normalizedType)) {
                    predicates.add(cb.equal(root.type(), Movie.class));
                } else if ("series".equals(normalizedType)) {
                    predicates.add(cb.equal(root.type(), TVSeries.class));
                }
            }

            if (request.getSeason() != null) {
                jakarta.persistence.criteria.Root<TVSeries> tvSeriesRoot = cb.treat(root, TVSeries.class);
                Join<TVSeries, Season> seasonJoin = tvSeriesRoot.join("seasons", JoinType.LEFT);
                predicates.add(cb.equal(seasonJoin.get("seasonNumber"), request.getSeason()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private List<ScoredVideo> paginateRanked(List<ScoredVideo> ranked, int page, int size) {
        int from = page * size;
        if (from >= ranked.size()) {
            return List.of();
        }
        int to = Math.min(from + size, ranked.size());
        return ranked.subList(from, to);
    }

    private double computeMatchScore(VideoContent video, String keyword, String keywordUnsigned) {
        if (keyword == null || keywordUnsigned == null) {
            return 1.0d;
        }

        String title = safe(video.getTitle()).toLowerCase(Locale.ROOT);
        String titleUnsigned = safe(video.getTitleUnsigned());
        if (titleUnsigned.isBlank()) {
            titleUnsigned = VietnameseNormalizer.normalize(video.getTitle());
        }

        if (title.equals(keyword.toLowerCase(Locale.ROOT)) || titleUnsigned.equals(keywordUnsigned)) {
            return 1.0d;
        }
        if (title.startsWith(keyword.toLowerCase(Locale.ROOT)) || titleUnsigned.startsWith(keywordUnsigned)) {
            return 0.93d;
        }
        if (title.contains(keyword.toLowerCase(Locale.ROOT)) || titleUnsigned.contains(keywordUnsigned)) {
            return 0.86d;
        }

        double levenshteinScore = 1.0d - (double) levenshtein(titleUnsigned, keywordUnsigned)
                / Math.max(titleUnsigned.length(), keywordUnsigned.length());

        double tokenCoverage = tokenCoverage(titleUnsigned, keywordUnsigned);
        return Math.max(levenshteinScore, tokenCoverage * 0.9d);
    }

    private double tokenCoverage(String titleUnsigned, String keywordUnsigned) {
        if (keywordUnsigned.isBlank()) {
            return 0.0d;
        }

        String[] queryTokens = keywordUnsigned.split(" ");
        int matched = 0;
        for (String token : queryTokens) {
            if (!token.isBlank() && titleUnsigned.contains(token)) {
                matched++;
            }
        }
        return (double) matched / queryTokens.length;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }

    private int sanitizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int sanitizeSize(Integer size) {
        if (size == null || size < 1) {
            return 24;
        }
        return Math.min(size, 50);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ScoredVideo(VideoContent video, double score) {
    }
}


