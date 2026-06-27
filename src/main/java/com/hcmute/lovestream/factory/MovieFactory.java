package com.hcmute.lovestream.factory;

import com.hcmute.lovestream.dto.request.contentmanager.movie.MovieUpsertRequest;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MovieFactory implements VideoContentFactory<MovieUpsertRequest, Movie> {

    private final GenreRepository genreRepository;

    @Override
    public Movie createContent(MovieUpsertRequest request, String posterUrl, String trailerUrl) {
        Movie movie = new Movie();
        
        // 1. Map các trường cơ bản từ VideoContent
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setReleaseYear(request.getReleaseYear());
        movie.setAgeRating(request.getAgeRating());
        movie.setQuality(request.getQuality());
        movie.setStatus(request.getStatus());
        movie.setCountry(request.getCountry());
        
        // 2. Map các trường đặc thù của Movie
        if (request.getReleaseDate() != null) {
            movie.setReleaseDate(java.util.Date.from(request.getReleaseDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        }
        movie.setDurationMinutes(request.getDurationMinutes());
        
        // 3. Xử lý Thể loại (Genres)
        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            List<Genre> genres = genreRepository.findAllById(request.getGenreIds());
            movie.setGenres(new HashSet<>(genres));
        }

        // 4. Xử lý MediaAsset (Poster, Trailer)
        List<MediaAsset> assets = new ArrayList<>();
        if (posterUrl != null && !posterUrl.isBlank()) {
            MediaAsset poster = new MediaAsset();
            poster.setAssetType(AssetType.POSTER);
            poster.setAssetUrl(posterUrl);
            poster.setVideoContent(movie);
            assets.add(poster);
        }
        
        if (trailerUrl != null && !trailerUrl.isBlank()) {
            MediaAsset trailer = new MediaAsset();
            trailer.setAssetType(AssetType.TRAILER);
            trailer.setAssetUrl(trailerUrl);
            trailer.setVideoContent(movie);
            assets.add(trailer);
        }
        movie.setMediaAssets(assets);
        
        // Lưu ý: Phần ContentCredit (Director, Cast) thường liên quan đến PersonRepository
        // nên có thể được xử lý riêng ở Service layer để Factory giữ được tính tinh gọn.
        
        return movie;
    }
}
