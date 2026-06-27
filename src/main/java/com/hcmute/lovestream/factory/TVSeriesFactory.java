package com.hcmute.lovestream.factory;

import com.hcmute.lovestream.dto.request.contentmanager.series.TVSeriesUpsertRequest;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Season;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TVSeriesFactory implements VideoContentFactory<TVSeriesUpsertRequest, TVSeries> {

    private final GenreRepository genreRepository;

    @Override
    public TVSeries createContent(TVSeriesUpsertRequest request, String posterUrl, String trailerUrl) {
        TVSeries tvSeries = new TVSeries();
        
        // 1. Map các trường cơ bản từ VideoContent
        tvSeries.setTitle(request.getTitle());
        tvSeries.setDescription(request.getDescription());
        tvSeries.setReleaseYear(request.getReleaseYear());
        tvSeries.setAgeRating(request.getAgeRating());
        tvSeries.setQuality(request.getQuality());
        tvSeries.setStatus(request.getStatus());
        
        // 2. Map các trường đặc thù của TVSeries
        tvSeries.setDurationMinutes(request.getDurationMinutes());
        
        // 3. Xử lý Thể loại (Genres)
        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            List<Genre> genres = genreRepository.findAllById(request.getGenreIds());
            tvSeries.setGenres(new HashSet<>(genres));
        }

        // 4. Xử lý MediaAsset (Poster, Trailer)
        List<MediaAsset> assets = new ArrayList<>();
        if (posterUrl != null && !posterUrl.isBlank()) {
            MediaAsset poster = new MediaAsset();
            poster.setAssetType(AssetType.POSTER);
            poster.setAssetUrl(posterUrl);
            poster.setVideoContent(tvSeries);
            assets.add(poster);
        }
        
        if (trailerUrl != null && !trailerUrl.isBlank()) {
            MediaAsset trailer = new MediaAsset();
            trailer.setAssetType(AssetType.TRAILER);
            trailer.setAssetUrl(trailerUrl);
            trailer.setVideoContent(tvSeries);
            assets.add(trailer);
        }
        tvSeries.setMediaAssets(assets);
        
        // 5. Tự động tạo sẵn cấu trúc Season mặc định đầu tiên
        Season season1 = new Season();
        season1.setSeasonNumber(1);
        season1.setName("Season 1");
        season1.setReleaseYear(request.getReleaseYear() != null ? request.getReleaseYear() : 0);
        season1.setTvSeries(tvSeries);
        
        List<Season> seasons = new ArrayList<>();
        seasons.add(season1);
        tvSeries.setSeasons(seasons);
        
        // Lưu ý: Phần ContentCredit (Director, Cast) thường được xử lý chung ở Service layer
        
        return tvSeries;
    }
}
