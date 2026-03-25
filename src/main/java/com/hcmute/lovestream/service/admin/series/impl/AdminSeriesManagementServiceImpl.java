package com.hcmute.lovestream.service.admin.series.impl;

import com.hcmute.lovestream.dto.request.admin.series.EpisodeUpsertRequest;
import com.hcmute.lovestream.dto.request.admin.series.SeasonUpsertRequest;
import com.hcmute.lovestream.dto.request.admin.series.TVSeriesUpsertRequest;
import com.hcmute.lovestream.entity.*;
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
import com.hcmute.lovestream.service.admin.series.AdminSeriesManagementService;
import com.hcmute.lovestream.service.storage.MediaStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSeriesManagementServiceImpl implements AdminSeriesManagementService {

    private final TVSeriesRepository tvSeriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final GenreRepository genreRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final PersonRepository personRepository;
    private final ContentCreditRepository contentCreditRepository;
    private final MediaStorageService mediaStorageService;

    // =====================================================================
    // TV Series CRUD
    // =====================================================================

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
                .orElseThrow(() -> new RuntimeException("Kh\u00f4ng t\u00ecm th\u1ea5y TV Series v\u1edbi ID: " + id));
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
        log.info("Deleting TV Series ID: {} (cascade: seasons, episodes, assets)", id);
        tvSeriesRepository.delete(series);
    }

    // =====================================================================
    // Season CRUD
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public Season getSeasonById(String id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kh\u00f4ng t\u00ecm th\u1ea5y Season v\u1edbi ID: " + id));
    }

    @Override
    @Transactional
    public Season createSeason(SeasonUpsertRequest request) {
        TVSeries series = getSeriesById(request.getTvSeriesId());

        // Validate unique seasonNumber within the same series (excluding self on update)
        if (seasonRepository.existsByTvSeriesAndSeasonNumber(series, request.getSeasonNumber())) {
            throw new RuntimeException("S\u1ED1 m\u00f9a " + request.getSeasonNumber() + " \u0111\u00e3 t\u1ED3n t\u1EA1i trong series n\u00e0y!");
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

        // If seasonNumber changed, check for conflict
        if (season.getSeasonNumber() != request.getSeasonNumber()) {
            if (seasonRepository.existsByTvSeriesAndSeasonNumber(season.getTvSeries(), request.getSeasonNumber())) {
                throw new RuntimeException("S\u1ED1 m\u00f9a " + request.getSeasonNumber() + " \u0111\u00e3 t\u1ED3n t\u1EA1i trong series n\u00e0y!");
            }
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
        seasonRepository.delete(season);
    }

    // =====================================================================
    // Episode CRUD
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public Episode getEpisodeById(String id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kh\u00f4ng t\u00ecm th\u1ea5y Episode v\u1edbi ID: " + id));
    }

    @Override
    @Transactional
    public Episode createEpisode(EpisodeUpsertRequest request) {
        Season season = getSeasonById(request.getSeasonId());

        if (episodeRepository.existsBySeasonAndEpisodeNumber(season, request.getEpisodeNumber())) {
            throw new RuntimeException("S\u1ED1 t\u1EADp " + request.getEpisodeNumber() + " \u0111\u00e3 t\u1ED3n t\u1EA1i trong m\u00f9a n\u00e0y!");
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

        if (episode.getEpisodeNumber() != request.getEpisodeNumber()) {
            if (episodeRepository.existsBySeasonAndEpisodeNumber(episode.getSeason(), request.getEpisodeNumber())) {
                throw new RuntimeException("S\u1ED1 t\u1EADp " + request.getEpisodeNumber() + " \u0111\u00e3 t\u1ED3n t\u1EA1i trong m\u00f9a n\u00e0y!");
            }
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
        episodeRepository.delete(episode);
    }

    // =====================================================================
    // Asset Management
    // =====================================================================

    @Override
    @Transactional
    public MediaAsset addSeriesAssetFromUrl(String seriesId, AssetType assetType, String url) {
        validateAssetType(assetType, AssetType.POSTER, AssetType.TRAILER);
        validateCloudinaryUrl(url);

        TVSeries series = getSeriesById(seriesId);
        MediaAsset asset = series.getMediaAssets().stream()
                .filter(a -> a.getAssetType() == assetType)
                .findFirst()
                .orElse(new MediaAsset());

        asset.setAssetType(assetType);
        asset.setAssetUrl(url);
        asset.setVideoContent(series);
        log.info("Saving {} asset to Series ID: {}", assetType, seriesId);
        return mediaAssetRepository.save(asset);
    }

    @Override
    @Transactional
    public MediaAsset addSeasonAssetFromUrl(String seasonId, String url) {
        validateCloudinaryUrl(url);
        Season season = getSeasonById(seasonId);

        MediaAsset asset = season.getMediaAssets().stream()
                .filter(a -> a.getAssetType() == AssetType.SEASON_POSTER)
                .findFirst()
                .orElse(new MediaAsset());

        asset.setAssetType(AssetType.SEASON_POSTER);
        asset.setAssetUrl(url);
        asset.setSeason(season);
        log.info("Saving SEASON_POSTER to Season ID: {}", seasonId);
        return mediaAssetRepository.save(asset);
    }

    @Override
    @Transactional
    public MediaAsset addEpisodeAssetFromUrl(String episodeId, String url) {
        validateCloudinaryUrl(url);
        Episode episode = getEpisodeById(episodeId);

        MediaAsset asset = episode.getMediaAssets().stream()
                .filter(a -> a.getAssetType() == AssetType.EPISODE_VIDEO)
                .findFirst()
                .orElse(new MediaAsset());

        asset.setAssetType(AssetType.EPISODE_VIDEO);
        asset.setAssetUrl(url);
        asset.setEpisode(episode);
        log.info("Saving EPISODE_VIDEO to Episode ID: {}", episodeId);
        return mediaAssetRepository.save(asset);
    }

    @Override
    @Transactional
    public MediaAsset uploadSeriesAsset(String seriesId, AssetType assetType, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng!");
        }
        String publicUrl = mediaStorageService.upload(file, assetType);
        return addSeriesAssetFromUrl(seriesId, assetType, publicUrl);
    }

    @Override
    @Transactional
    public MediaAsset uploadSeasonAsset(String seasonId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng!");
        }
        String publicUrl = mediaStorageService.upload(file, AssetType.SEASON_POSTER);
        return addSeasonAssetFromUrl(seasonId, publicUrl);
    }

    @Override
    @Transactional
    public MediaAsset uploadEpisodeAsset(String episodeId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng!");
        }
        String publicUrl = mediaStorageService.upload(file, AssetType.EPISODE_VIDEO);
        return addEpisodeAssetFromUrl(episodeId, publicUrl);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

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
                throw new RuntimeException("C\u00f3 \u00edt nh\u1EA5t m\u1ED9t th\u1EC3 lo\u1EA1i kh\u00f4ng t\u1ED3n t\u1EA1i trong h\u1EC7 th\u1ED1ng. Vui l\u00f2ng t\u1EA3i l\u1EA1i trang!");
            }
            target.setGenres(new HashSet<>(genres));
        } else {
            target.setGenres(new HashSet<>());
        }
    }

    /** Rewrite directors and cast for a series based on comma-separated name strings. */
    private void syncCredits(TVSeries series, String directorNamesRaw, String castNamesRaw) {
        // Remove all existing credits for this series
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
        if (namesRaw == null || namesRaw.isBlank()) return Collections.emptyList();

        return Arrays.stream(namesRaw.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .map(name -> {
                    // Find or create person
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

    private void validateCloudinaryUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL kh\u00f4ng h\u1EE3p l\u1EC7. \u0110\u01B0\u1EddNG d\u1EAbn kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng!");
        }
        if (!url.contains("res.cloudinary.com")) {
            throw new IllegalArgumentException("URL kh\u00f4ng h\u1EE3p l\u1EC7. Ch\u1EC9 ch\u1EA5p nh\u1EADn link public t\u1EEB n\u1EC1n t\u1EA3ng Cloudinary.");
        }
    }

    private void validateAssetType(AssetType provided, AssetType... allowed) {
        for (AssetType a : allowed) {
            if (provided == a) return;
        }
        throw new IllegalArgumentException("Lo\u1EA1i asset kh\u00f4ng h\u1EE3p l\u1EC7 cho t\u00e0i nguy\u00ean n\u00e0y!");
    }
}
