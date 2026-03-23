package com.hcmute.lovestream.service.admin.movie;

import com.hcmute.lovestream.dto.request.admin.movie.MovieUpsertRequest;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AdminMovieManagementService {
    
    // -- 1. Queries --
    List<Movie> getAllMovies();
    List<Movie> getMoviesByStatus(ContentStatus status);
    List<Movie> searchMoviesByTitle(String keyword);
    List<Movie> filterMovies(ContentStatus status, String keyword);
    Movie getMovieById(String id);

    // -- 2. CRUD Operations --
    Movie createMovie(MovieUpsertRequest request);
    Movie updateMovie(String id, MovieUpsertRequest request);

    // -- 3. Status Management --
    void hideMovie(String id);
    void restoreMovie(String id);

    // -- 4. Media Asset Management (URL-based from existing service) --
    MediaAsset addAsset(String movieId, AssetType assetType, String assetUrl);
    void removeAsset(String movieId, String assetId);

    // -- 5. Upload Media Asset (triggers real file upload + addAsset) --
    MediaAsset uploadMovieAsset(String movieId, AssetType assetType, MultipartFile file) throws IOException;
}

