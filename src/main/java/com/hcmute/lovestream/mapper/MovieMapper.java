package com.hcmute.lovestream.mapper;

import com.hcmute.lovestream.dto.response.MovieResponse;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AssetType;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MovieMapper {

    public MovieResponse toMovieResponse(Movie movie) {
        String posterUrl = movie.getMediaAssets().stream()
                .filter(a -> a.getAssetType() == AssetType.POSTER)
                .map(MediaAsset::getAssetUrl)
                .findFirst()
                .orElse("https://via.placeholder.com/300x450");

        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .imagePosterUrl(posterUrl)
                .build();
    }
}
