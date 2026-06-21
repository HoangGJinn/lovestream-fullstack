package com.hcmute.lovestream.service.contentmanager.movie.impl;

import com.hcmute.lovestream.dto.request.contentmanager.movie.MovieUpsertRequest;
import com.hcmute.lovestream.entity.ContentCredit;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.Person;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.CreditType;
import com.hcmute.lovestream.repository.ContentCreditRepository;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.repository.MediaAssetRepository;
import com.hcmute.lovestream.repository.MovieRepository;
import com.hcmute.lovestream.repository.PersonRepository;
import com.hcmute.lovestream.service.contentmanager.movie.ContentManagerMovieManagementService;
import com.hcmute.lovestream.service.storage.CloudinaryFolderTarget;
import com.hcmute.lovestream.service.storage.MediaStorageService;
import com.hcmute.lovestream.factory.MovieFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentManagerMovieManagementServiceImpl implements ContentManagerMovieManagementService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final PersonRepository personRepository;
    private final ContentCreditRepository contentCreditRepository;
    private final MediaStorageService mediaStorageService;
    private final MovieFactory movieFactory;

    // -- 1. Queries (Giữ code gộp hàm của BẠN) --
    @Override
    @Transactional(readOnly = true)
    public Page<Movie> getMovies(String keyword, ContentStatus status, Pageable pageable) {
        if (keyword != null && !keyword.isBlank() && status != null) {
            return movieRepository.findByTitleContainingIgnoreCaseAndStatus(keyword.trim(), status, pageable);
        }
        if (status != null) {
            return movieRepository.findByStatus(status, pageable);
        }
        if (keyword != null && !keyword.isBlank()) {
            return movieRepository.findByTitleContainingIgnoreCase(keyword.trim(), pageable);
        }
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
        // Sử dụng MovieFactory để khởi tạo thay vì map thủ công
        // Lưu ý: posterUrl và trailerUrl có thể truyền null nếu hệ thống 
        // hiện tại đang upload riêng biệt qua API khác.
        Movie movie = movieFactory.createContent(request, null, null);
        log.info("Creating new Movie: {}", request.getTitle());
        Movie saved = movieRepository.save(movie);
        syncCredits(saved, request.getDirectorNames(), request.getCastNames());
        return saved;
    }

    @Override
    @Transactional
    public Movie updateMovie(String id, MovieUpsertRequest request) {
        Movie targetMovie = getMovieById(id);
        mapRequestToMovie(request, targetMovie);
        log.info("Updating Movie ID: {}", id);
        Movie saved = movieRepository.save(targetMovie);
        syncCredits(saved, request.getDirectorNames(), request.getCastNames());
        return saved;
    }

    // -- 3. Status Management --
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
    }

    // -- 4. Media Asset Management --
    @Override
    @Transactional
    public MediaAsset uploadMoviePoster(String movieId, MultipartFile file) throws IOException {
        validatePosterUpload(file);
        String publicUrl = mediaStorageService.upload(file, CloudinaryFolderTarget.MOVIE_POSTER);
        return upsertMovieAsset(movieId, AssetType.POSTER, publicUrl);
    }

    @Override
    @Transactional
    public MediaAsset addMovieTrailerFromUrl(String movieId, String assetUrl) {
        validatePublicVideoUrl(assetUrl);
        return upsertMovieAsset(movieId, AssetType.TRAILER, assetUrl);
    }

    @Override
    @Transactional
    public MediaAsset addMovieVideoFromUrl(String movieId, String assetUrl) {
        validatePublicVideoUrl(assetUrl);
        return upsertMovieAsset(movieId, AssetType.FULL_VIDEO, assetUrl);
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

    // --- HELPER METHOD ---
    private void mapRequestToMovie(MovieUpsertRequest request, Movie target) {
        target.setTitle(request.getTitle());
        target.setDescription(request.getDescription());
        target.setReleaseYear(request.getReleaseYear());

        if (request.getReleaseDate() != null) {
            target.setReleaseDate(java.util.Date.from(request.getReleaseDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        }

        target.setDurationMinutes(request.getDurationMinutes());
        target.setAgeRating(request.getAgeRating());
        target.setQuality(request.getQuality());
        target.setStatus(request.getStatus());
        target.setCountry(request.getCountry());

        List<String> genreIds = request.getGenreIds();
        if (genreIds != null && !genreIds.isEmpty()) {
            List<Genre> selectedGenres = genreRepository.findAllById(genreIds);

            if (selectedGenres.isEmpty() || selectedGenres.size() != genreIds.size()) {
                throw new RuntimeException(
                        "Có ít nhất một thể loại không tồn tại trong hệ thống. Vui lòng tải lại trang!");
            }
            target.setGenres(new HashSet<>(selectedGenres));
        } else {
            target.setGenres(new HashSet<>());
        }
    }

    /** Rewrite directors and cast for a movie based on comma-separated name strings. */
    private void syncCredits(Movie movie, String directorNamesRaw, String castNamesRaw) {
        // Remove all existing credits for this movie
        List<ContentCredit> existing = movie.getContentCredits();
        if (existing != null && !existing.isEmpty()) {
            contentCreditRepository.deleteAll(existing);
        }

        List<ContentCredit> newCredits = new ArrayList<>();
        newCredits.addAll(buildCredits(movie, directorNamesRaw, CreditType.DIRECTOR));
        newCredits.addAll(buildCredits(movie, castNamesRaw, CreditType.CAST));
        contentCreditRepository.saveAll(newCredits);
    }

    private List<ContentCredit> buildCredits(Movie movie, String namesRaw, CreditType creditType) {
        if (namesRaw == null || namesRaw.isBlank())
            return Collections.emptyList();

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
                    credit.setVideoContent(movie);
                    return credit;
                })
                .collect(Collectors.toList());
    }

    private MediaAsset upsertMovieAsset(String movieId, AssetType assetType, String assetUrl) {
        Movie movie = getMovieById(movieId);

        List<MediaAsset> assets = movie.getMediaAssets() == null ? List.of() : movie.getMediaAssets();
        MediaAsset asset = assets.stream()
                .filter(a -> a.getAssetType() == assetType)
                .findFirst()
                .orElse(new MediaAsset());

        asset.setAssetType(assetType);
        asset.setAssetUrl(assetUrl == null ? null : assetUrl.trim());
        asset.setVideoContent(movie);

        log.info("Saving {} to Movie ID: {}", assetType, movieId);
        return mediaAssetRepository.save(asset);
    }

    private void validatePosterUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File poster không được để trống!");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Poster phải là file ảnh hợp lệ.");
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
