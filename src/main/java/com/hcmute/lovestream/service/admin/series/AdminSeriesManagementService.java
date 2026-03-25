package com.hcmute.lovestream.service.admin.series;

import com.hcmute.lovestream.dto.request.admin.series.EpisodeUpsertRequest;
import com.hcmute.lovestream.dto.request.admin.series.SeasonUpsertRequest;
import com.hcmute.lovestream.dto.request.admin.series.TVSeriesUpsertRequest;
import com.hcmute.lovestream.entity.Episode;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Season;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminSeriesManagementService {

    // ---- TV Series ----
    Page<TVSeries> getSeries(String keyword, ContentStatus status, Pageable pageable);
    TVSeries getSeriesById(String id);
    TVSeries createSeries(TVSeriesUpsertRequest request);
    TVSeries updateSeries(String id, TVSeriesUpsertRequest request);
    void toggleSeriesStatus(String id);
    void deleteSeries(String id);

    // ---- Season ----
    Season getSeasonById(String id);
    Season createSeason(SeasonUpsertRequest request);
    Season updateSeason(String id, SeasonUpsertRequest request);
    void deleteSeason(String id);

    // ---- Episode ----
    Episode getEpisodeById(String id);
    Episode createEpisode(EpisodeUpsertRequest request);
    Episode updateEpisode(String id, EpisodeUpsertRequest request);
    void deleteEpisode(String id);

    // ---- Assets ----
    MediaAsset uploadSeriesPoster(String seriesId, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException;
    MediaAsset addSeriesTrailerFromUrl(String seriesId, String url);
    MediaAsset addEpisodeVideoFromUrl(String episodeId, String url);
}
