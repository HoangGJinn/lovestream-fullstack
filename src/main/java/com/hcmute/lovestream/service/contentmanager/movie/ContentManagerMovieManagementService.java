package com.hcmute.lovestream.service.contentmanager.movie;

import com.hcmute.lovestream.dto.request.contentmanager.movie.MovieUpsertRequest;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ContentManagerMovieManagementService {

    // -- 1. Queries (Giữ code gộp chung của BẠN) --
    // Hàm này cân hết: Lấy tất cả, Lấy theo từ khóa, Lấy theo trạng thái
    Page<Movie> getMovies(String keyword, ContentStatus status, Pageable pageable);

    Movie getMovieById(String id);

    // -- 2. CRUD Operations --
    Movie createMovie(MovieUpsertRequest request);

    Movie updateMovie(String id, MovieUpsertRequest request);

    // -- 3. Status Management (Gộp thành 1 hàm duy nhất cho đồng bộ với
    // User/Voucher) --
    void toggleMovieStatus(String id);

    // -- 4. Media Asset Management --
    MediaAsset uploadMoviePoster(String movieId, MultipartFile file) throws IOException;
    MediaAsset addMovieTrailerFromUrl(String movieId, String assetUrl);
    MediaAsset addMovieVideoFromUrl(String movieId, String assetUrl);
    void removeAsset(String movieId, String assetId);
}
