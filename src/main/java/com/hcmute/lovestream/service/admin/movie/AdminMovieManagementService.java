package com.hcmute.lovestream.service.admin.movie;

import com.hcmute.lovestream.dto.request.admin.movie.MovieUpsertRequest;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AdminMovieManagementService {

    // -- 1. Queries (Đã gộp chung và tích hợp Phân trang) --
    // Hàm này cân hết: Lấy tất cả, Lấy theo từ khóa, Lấy theo trạng thái
    Page<Movie> getMovies(String keyword, ContentStatus status, Pageable pageable);

    Movie getMovieById(String id);

    // -- 2. CRUD Operations --
    Movie createMovie(MovieUpsertRequest request);
    Movie updateMovie(String id, MovieUpsertRequest request);

    // -- 3. Status Management (Gộp thành 1 hàm duy nhất cho đồng bộ với User/Voucher) --
    void toggleMovieStatus(String id);

    // -- 4. Media Asset Management (URL-based) --
    MediaAsset addAsset(String movieId, AssetType assetType, String assetUrl);
    void removeAsset(String movieId, String assetId);

    // -- 5. Upload Media Asset (Lưu file vật lý/Cloud + addAsset) --
    MediaAsset uploadMovieAsset(String movieId, AssetType assetType, MultipartFile file) throws IOException;
}