package com.hcmute.lovestream.service.admin.movie.impl;

import com.hcmute.lovestream.dto.request.admin.movie.MovieUpsertRequest;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.repository.MediaAssetRepository;
import com.hcmute.lovestream.repository.MovieRepository;
import com.hcmute.lovestream.service.admin.movie.AdminMovieManagementService;
import com.hcmute.lovestream.service.storage.MediaStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // Đã thêm import
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMovieManagementServiceImpl implements AdminMovieManagementService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaStorageService mediaStorageService;

    // -- 1. Queries (Đã gộp chung và tích hợp Phân trang) --
    @Override
    @Transactional(readOnly = true)
    public Page<Movie> getMovies(String keyword, ContentStatus status, Pageable pageable) {
        // Nếu có cả từ khóa và trạng thái
        if (keyword != null && !keyword.isBlank() && status != null) {
            return movieRepository.findByTitleContainingIgnoreCaseAndStatus(keyword.trim(), status, pageable);
        }
        // Nếu chỉ có trạng thái
        if (status != null) {
            return movieRepository.findByStatus(status, pageable);
        }
        // Nếu chỉ có từ khóa
        if (keyword != null && !keyword.isBlank()) {
            return movieRepository.findByTitleContainingIgnoreCase(keyword.trim(), pageable);
        }
        // Lấy tất cả (Mặc định)
        return movieRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Movie getMovieById(String id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim lẻ với ID: " + id));
    }

    // -- 2. CRUD Operations --
    @Override
    @Transactional
    public Movie createMovie(MovieUpsertRequest request) {
        Movie movie = new Movie();
        mapRequestToMovie(request, movie);

        // Đảm bảo không ghi đè ID (ID sẽ do Spring Data/JPA tự sinh UUID)
        movie.setId(null);

        log.info("Creating new Movie: {}", request.getTitle());
        return movieRepository.save(movie);
    }

    @Override
    @Transactional
    public Movie updateMovie(String id, MovieUpsertRequest request) {
        Movie targetMovie = getMovieById(id);

        mapRequestToMovie(request, targetMovie);

        log.info("Updating Movie ID: {}", id);
        return movieRepository.save(targetMovie);
    }

    // -- 3. Status Management (Đã gộp thành 1 hàm toggle) --
    @Override
    @Transactional
    public void toggleMovieStatus(String id) {
        Movie movie = getMovieById(id);

        if (movie.getStatus() == ContentStatus.ACTIVE) {
            movie.setStatus(ContentStatus.HIDDEN);
            log.info("Hidden Movie ID: {}", id);
        } else {
            movie.setStatus(ContentStatus.ACTIVE);
            log.info("Restored Movie ID: {}", id);
        }
        // Nhờ @Transactional, DB sẽ tự động update
    }

    // -- 4. Media Asset Management --
    @Override
    @Transactional
    public MediaAsset addAsset(String movieId, AssetType assetType, String assetUrl) {
        Movie movie = getMovieById(movieId);

        MediaAsset asset = new MediaAsset();
        asset.setAssetType(assetType);
        asset.setAssetUrl(assetUrl);
        asset.setVideoContent(movie); // Kế thừa từ VideoContent

        log.info("Adding {} to Movie ID: {}", assetType, movieId);
        return mediaAssetRepository.save(asset);
    }

    @Override
    @Transactional
    public void removeAsset(String movieId, String assetId) {
        getMovieById(movieId);

        MediaAsset asset = mediaAssetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Asset với ID: " + assetId));

        if (asset.getVideoContent() == null || !asset.getVideoContent().getId().equals(movieId)) {
            throw new RuntimeException("Tài nguyên không thuộc về bộ phim này!");
        }

        log.info("Removing Asset {} from Movie ID: {}", assetId, movieId);
        mediaAssetRepository.delete(asset);
    }

    @Override
    @Transactional
    public MediaAsset uploadMovieAsset(String movieId, AssetType assetType, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload không được để trống!");
        }

        // 1. Upload lên Cloudinary → lấy URL
        String publicUrl = mediaStorageService.upload(file, assetType);

        // 2. Tạo MediaAsset và gắn vào Movie
        return addAsset(movieId, assetType, publicUrl);
    }

    // --- HELPER METHOD ---
    private void mapRequestToMovie(MovieUpsertRequest request, Movie target) {
        target.setTitle(request.getTitle());
        target.setDescription(request.getDescription());
        target.setReleaseYear(request.getReleaseYear());

        if (request.getReleaseDate() != null) {
            target.setReleaseDate(java.sql.Date.valueOf(request.getReleaseDate()));
        }

        target.setDurationMinutes(request.getDurationMinutes());
        target.setAgeRating(request.getAgeRating());
        target.setQuality(request.getQuality());
        target.setStatus(request.getStatus());

        List<String> genreIds = request.getGenreIds();
        if (genreIds != null && !genreIds.isEmpty()) {
            List<Genre> selectedGenres = genreRepository.findAllById(genreIds);

            if (selectedGenres.isEmpty() || selectedGenres.size() != genreIds.size()) {
                throw new RuntimeException("Có ít nhất một thể loại không tồn tại trong hệ thống. Vui lòng tải lại trang!");
            }

            target.setGenres(new HashSet<>(selectedGenres));
        } else {
            target.setGenres(new HashSet<>());
        }
    }
}