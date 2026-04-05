package com.hcmute.lovestream.service.contentmanager.series.impl;

import com.hcmute.lovestream.dto.request.contentmanager.series.EpisodeUpsertRequest;
import com.hcmute.lovestream.dto.request.contentmanager.series.SeasonUpsertRequest;
import com.hcmute.lovestream.dto.request.contentmanager.series.TVSeriesUpsertRequest;
import com.hcmute.lovestream.entity.ContentCredit;
import com.hcmute.lovestream.entity.Episode;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Person;
import com.hcmute.lovestream.entity.Season;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.CreditType;
import com.hcmute.lovestream.repository.ContentCreditRepository;
import com.hcmute.lovestream.repository.EpisodeRepository;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.repository.MediaAssetRepository;
import com.hcmute.lovestream.repository.PersonRepository;
import com.hcmute.lovestream.repository.SeasonRepository;
import com.hcmute.lovestream.repository.TVSeriesRepository;
import com.hcmute.lovestream.repository.UserSeriesWatchStateRepository;
import com.hcmute.lovestream.repository.WatchHistoryRepository;
import com.hcmute.lovestream.service.contentmanager.series.ContentManagerSeriesManagementService;
import com.hcmute.lovestream.service.notification.SeriesReleaseNotificationService;
import com.hcmute.lovestream.service.storage.CloudinaryFolderTarget;
import com.hcmute.lovestream.service.storage.MediaStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentManagerSeriesManagementServiceImpl implements ContentManagerSeriesManagementService {

    private static final long MAX_SERIES_POSTER_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_SERIES_POSTER_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_SERIES_POSTER_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp"
    );

    private final TVSeriesRepository tvSeriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final GenreRepository genreRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final UserSeriesWatchStateRepository userSeriesWatchStateRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final PersonRepository personRepository;
    private final ContentCreditRepository contentCreditRepository;
    private final MediaStorageService mediaStorageService;
    private final SeriesReleaseNotificationService seriesReleaseNotificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<TVSeries> getSeries(String keyword, ContentStatus status, Pageable pageable) {
        if (keyword != null && !keyword.isBlank() && status != null) {
            return tvSeriesRepository.findByTitleContainingIgnoreCaseAndStatus(keyword.trim(), status, pageable);
        }
        if (status != null) {
            return tvSeriesRepository.findByStatus(status, pageable);
        }
        if (keyword != null && !keyword.isBlank()) {
            return tvSeriesRepository.findByTitleContainingIgnoreCase(keyword.trim(), pageable);
        }
        return tvSeriesRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public TVSeries getSeriesById(String id) {
        return tvSeriesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy TV Series với ID: " + id));
    }

    @Override
    @Transactional
    public TVSeries createSeries(TVSeriesUpsertRequest request) {
        TVSeries series = new TVSeries();
        mapRequestToSeries(request, series);
        series.setId(null);
        log.info("Creating TV Series: {}", request.getTitle());
        TVSeries saved = tvSeriesRepository.save(series);
        syncCredits(saved, request.getDirectorNames(), request.getCastNames());
        return saved;
    }

    @Override
    @Transactional
    public TVSeries updateSeries(String id, TVSeriesUpsertRequest request) {
        TVSeries series = getSeriesById(id);
        mapRequestToSeries(request, series);
        log.info("Updating TV Series ID: {}", id);
        TVSeries saved = tvSeriesRepository.save(series);
        syncCredits(saved, request.getDirectorNames(), request.getCastNames());
        return saved;
    }

    @Override
    @Transactional
    public void toggleSeriesStatus(String id) {
        TVSeries series = getSeriesById(id);
        if (series.getStatus() == ContentStatus.ACTIVE) {
            series.setStatus(ContentStatus.HIDDEN);
        } else {
            series.setStatus(ContentStatus.ACTIVE);
        }
        tvSeriesRepository.save(series);
        log.info("Toggled status for TV Series ID: {}", id);
    }

    @Override
    @Transactional
    public void deleteSeries(String id) {
        TVSeries series = getSeriesById(id);
        log.info("Deleting TV Series ID: {} (cascade: seasons, episodes, assets, watch_history)", id);
        watchHistoryRepository.deleteByVideoContentId(series.getId());
        userSeriesWatchStateRepository.deleteBySeries_Id(series.getId());
        tvSeriesRepository.delete(series);
    }

    @Override
    @Transactional(readOnly = true)
    public Season getSeasonById(String id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Season với ID: " + id));
    }

    @Override
    @Transactional
    public Season createSeason(SeasonUpsertRequest request) {
        TVSeries series = getSeriesById(request.getTvSeriesId());

        if (seasonRepository.existsByTvSeriesAndSeasonNumber(series, request.getSeasonNumber())) {
            throw new RuntimeException("Số mùa " + request.getSeasonNumber() + " đã tồn tại trong series này!");
        }

        Season season = new Season();
        season.setTvSeries(series);
        season.setSeasonNumber(request.getSeasonNumber());
        season.setName(request.getName());
        season.setReleaseYear(request.getReleaseYear() != null ? request.getReleaseYear() : 0);
        log.info("Creating Season {} for TV Series ID: {}", request.getSeasonNumber(), series.getId());
        return seasonRepository.save(season);
    }

    @Override
    @Transactional
    public Season updateSeason(String id, SeasonUpsertRequest request) {
        Season season = getSeasonById(id);

        if (season.getSeasonNumber() != request.getSeasonNumber()
                && seasonRepository.existsByTvSeriesAndSeasonNumber(season.getTvSeries(), request.getSeasonNumber())) {
            throw new RuntimeException("Số mùa " + request.getSeasonNumber() + " đã tồn tại trong series này!");
        }

        season.setSeasonNumber(request.getSeasonNumber());
        season.setName(request.getName());
        season.setReleaseYear(request.getReleaseYear() != null ? request.getReleaseYear() : 0);
        log.info("Updating Season ID: {}", id);
        return seasonRepository.save(season);
    }

    @Override
    @Transactional
    public void deleteSeason(String id) {
        Season season = getSeasonById(id);
        log.info("Deleting Season ID: {} (cascade: episodes, assets)", id);
        userSeriesWatchStateRepository.clearLastWatchedEpisodeBySeasonId(season.getId());
        seasonRepository.delete(season);
    }

    @Override
    @Transactional(readOnly = true)
    public Episode getEpisodeById(String id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Episode với ID: " + id));
    }

    @Override
    @Transactional
    public Episode createEpisode(EpisodeUpsertRequest request) {
        Season season = getSeasonById(request.getSeasonId());

        if (episodeRepository.existsBySeasonAndEpisodeNumber(season, request.getEpisodeNumber())) {
            throw new RuntimeException("Số tập " + request.getEpisodeNumber() + " đã tồn tại trong mùa này!");
        }

        Episode episode = new Episode();
        episode.setSeason(season);
        episode.setEpisodeNumber(request.getEpisodeNumber());
        episode.setTitle(request.getTitle());
        episode.setDurationInMinutes(request.getDurationInMinutes());
        if (request.getAirDate() != null) {
            episode.setAirDate(java.sql.Date.valueOf(request.getAirDate()));
        }
        log.info("Creating Episode {} for Season ID: {}", request.getEpisodeNumber(), season.getId());
        return episodeRepository.save(episode);
    }

    @Override
    @Transactional
    public Episode updateEpisode(String id, EpisodeUpsertRequest request) {
        Episode episode = getEpisodeById(id);

        if (episode.getEpisodeNumber() != request.getEpisodeNumber()
                && episodeRepository.existsBySeasonAndEpisodeNumber(episode.getSeason(), request.getEpisodeNumber())) {
            throw new RuntimeException("Số tập " + request.getEpisodeNumber() + " đã tồn tại trong mùa này!");
        }

        episode.setEpisodeNumber(request.getEpisodeNumber());
        episode.setTitle(request.getTitle());
        episode.setDurationInMinutes(request.getDurationInMinutes());
        if (request.getAirDate() != null) {
            episode.setAirDate(java.sql.Date.valueOf(request.getAirDate()));
        }
        log.info("Updating Episode ID: {}", id);
        return episodeRepository.save(episode);
    }

    @Override
    @Transactional
    public void deleteEpisode(String id) {
        Episode episode = getEpisodeById(id);
        log.info("Deleting Episode ID: {}", id);
        userSeriesWatchStateRepository.clearLastWatchedEpisode(episode.getId());
        episodeRepository.delete(episode);
    }

    @Override
    @Transactional
    public MediaAsset uploadSeriesPoster(String seriesId, MultipartFile file) throws IOException {
        validateSeriesPosterUpload(file);
        String publicUrl = mediaStorageService.upload(file, CloudinaryFolderTarget.SERIES_POSTER);

        TVSeries series = getSeriesById(seriesId);
        List<MediaAsset> assets = series.getMediaAssets() == null ? List.of() : series.getMediaAssets();
        MediaAsset asset = assets.stream()
                .filter(a -> a.getAssetType() == AssetType.POSTER)
                .findFirst()
                .orElse(new MediaAsset());

        asset.setAssetType(AssetType.POSTER);
        asset.setAssetUrl(publicUrl);
        asset.setVideoContent(series);
        log.info("Saving POSTER to Series ID: {}", seriesId);
        return mediaAssetRepository.save(asset);
    }

    @Override
    @Transactional
    public MediaAsset addSeriesTrailerFromUrl(String seriesId, String url) {
        validatePublicVideoUrl(url);
        TVSeries series = getSeriesById(seriesId);
        List<MediaAsset> assets = series.getMediaAssets() == null ? List.of() : series.getMediaAssets();

        MediaAsset asset = assets.stream()
                .filter(a -> a.getAssetType() == AssetType.TRAILER)
                .findFirst()
                .orElse(new MediaAsset());

        asset.setAssetType(AssetType.TRAILER);
        asset.setAssetUrl(url == null ? null : url.trim());
        asset.setVideoContent(series);
        log.info("Saving TRAILER URL to Series ID: {}", seriesId);
        return mediaAssetRepository.save(asset);
    }

    @Override
    @Transactional
    public MediaAsset addEpisodeVideoFromUrl(String episodeId, String url) {
        validatePublicVideoUrl(url);
        Episode episode = getEpisodeById(episodeId);
        List<MediaAsset> assets = episode.getMediaAssets() == null ? List.of() : episode.getMediaAssets();

        MediaAsset asset = assets.stream()
                .filter(a -> a.getAssetType() == AssetType.EPISODE_VIDEO)
                .findFirst()
                .orElse(new MediaAsset());

        asset.setAssetType(AssetType.EPISODE_VIDEO);
        asset.setAssetUrl(url == null ? null : url.trim());
        asset.setEpisode(episode);
        log.info("Saving EPISODE_VIDEO URL to Episode ID: {}", episodeId);
        MediaAsset savedAsset = mediaAssetRepository.save(asset);
        seriesReleaseNotificationService.notifyEpisodeAvailable(episode);
        return savedAsset;
    }

    private void mapRequestToSeries(TVSeriesUpsertRequest request, TVSeries target) {
        target.setTitle(request.getTitle());
        target.setDescription(request.getDescription());
        target.setReleaseYear(request.getReleaseYear());
        target.setAgeRating(request.getAgeRating());
        target.setQuality(request.getQuality());
        target.setStatus(request.getStatus());
        target.setDurationMinutes(request.getDurationMinutes());

        List<String> genreIds = request.getGenreIds();
        if (genreIds != null && !genreIds.isEmpty()) {
            List<Genre> genres = genreRepository.findAllById(genreIds);
            if (genres.size() != genreIds.size()) {
                throw new RuntimeException("Có ít nhất một thể loại không tồn tại trong hệ thống. Vui lòng tải lại trang!");
            }
            target.setGenres(new HashSet<>(genres));
        } else {
            target.setGenres(new HashSet<>());
        }
    }

    private void syncCredits(TVSeries series, String directorNamesRaw, String castNamesRaw) {
        List<ContentCredit> existing = series.getContentCredits();
        if (existing != null && !existing.isEmpty()) {
            contentCreditRepository.deleteAll(existing);
        }

        List<ContentCredit> newCredits = new ArrayList<>();
        newCredits.addAll(buildCredits(series, directorNamesRaw, CreditType.DIRECTOR));
        newCredits.addAll(buildCredits(series, castNamesRaw, CreditType.CAST));
        contentCreditRepository.saveAll(newCredits);
    }

    private List<ContentCredit> buildCredits(TVSeries series, String namesRaw, CreditType creditType) {
        if (namesRaw == null || namesRaw.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(namesRaw.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .map(name -> {
                    Person person = personRepository.findByFullNameContainingIgnoreCase(name)
                            .stream()
                            .filter(p -> p.getCreditType() == creditType && p.getFullName().equalsIgnoreCase(name))
                            .findFirst()
                            .orElseGet(() -> {
                                Person p = new Person();
                                p.setFullName(name);
                                p.setCreditType(creditType);
                                return personRepository.save(p);
                            });

                    ContentCredit credit = new ContentCredit();
                    credit.setCreditType(creditType);
                    credit.setPerson(person);
                    credit.setVideoContent(series);
                    return credit;
                })
                .collect(Collectors.toList());
    }

    private void validateSeriesPosterUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File poster không được để trống!");
        }

        if (file.getSize() > MAX_SERIES_POSTER_SIZE_BYTES) {
            throw new IllegalArgumentException("Poster không được vượt quá 5MB.");
        }

        String contentType = Optional.ofNullable(file.getContentType())
                .map(type -> type.toLowerCase(Locale.ROOT))
                .orElse("");
        String originalFilename = Optional.ofNullable(file.getOriginalFilename())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .orElse("");

        boolean validContentType = ALLOWED_SERIES_POSTER_CONTENT_TYPES.contains(contentType);
        boolean validExtension = ALLOWED_SERIES_POSTER_EXTENSIONS.stream()
                .anyMatch(originalFilename::endsWith);

        if (!validContentType && !validExtension) {
            throw new IllegalArgumentException("Poster chỉ hỗ trợ định dạng JPG, PNG hoặc WEBP.");
        }
    }

    private void validatePublicVideoUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL không hợp lệ. Đường dẫn không được để trống!");
        }

        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("URL không hợp lệ. Đường dẫn phải đúng định dạng http/https.", ex);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("URL không hợp lệ. Chỉ chấp nhận link http/https công khai.");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("URL không hợp lệ. Thiếu tên miền ở đường dẫn video.");
        }
    }
}
