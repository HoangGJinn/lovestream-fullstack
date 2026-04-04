package com.hcmute.lovestream.controller.web.contentmanager;

import com.hcmute.lovestream.dto.request.contentmanager.movie.MovieUpsertRequest;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.CreditType;
import com.hcmute.lovestream.entity.enums.Quality;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.service.contentmanager.movie.ContentManagerMovieManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/content-manager/movies")
@RequiredArgsConstructor
@Slf4j
public class ContentManagerMovieController {

    private final ContentManagerMovieManagementService movieManagementService;
    private final GenreRepository genreRepository;

    @ModelAttribute("allGenres")
    public List<Genre> populateGenres() {
        return genreRepository.findAll();
    }

    @ModelAttribute("allAgeRatings")
    public AgeRating[] populateAgeRatings() {
        return AgeRating.values();
    }

    @ModelAttribute("allQualities")
    public Quality[] populateQualities() {
        return Quality.values();
    }

    @ModelAttribute("allStatuses")
    public ContentStatus[] populateStatuses() {
        return ContentStatus.values();
    }

    @GetMapping
    public String listMovies(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Movie> moviePage = movieManagementService.getMovies(keyword, status, pageable);

        model.addAttribute("movies", moviePage.getContent());
        model.addAttribute("moviePage", moviePage);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("totalPages", moviePage.getTotalPages());
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentKeyword", keyword);

        return "content-manager/movies/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("movieUpsertRequest")) {
            model.addAttribute("movieUpsertRequest", new MovieUpsertRequest());
        }
        return "content-manager/movies/form";
    }

    @PostMapping
    public String createMovie(
            @Valid @ModelAttribute("movieUpsertRequest") MovieUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "content-manager/movies/form";
        }

        try {
            Movie created = movieManagementService.createMovie(request);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Tạo phim thành công! Tiếp tục tải lên poster/trailer/video nếu cần.");
            return "redirect:/content-manager/movies/" + created.getId() + "/edit";
        } catch (RuntimeException e) {
            log.error("Lỗi khi tạo movie", e);
            bindingResult.reject("error.movie", e.getMessage());
            return "content-manager/movies/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Movie movie = movieManagementService.getMovieById(id);

            if (!model.containsAttribute("movieUpsertRequest")) {
                MovieUpsertRequest request = MovieUpsertRequest.builder()
                        .id(movie.getId())
                        .title(movie.getTitle())
                        .description(movie.getDescription())
                        .releaseYear(movie.getReleaseYear())
                        .releaseDate(movie.getReleaseDate() != null
                                ? new java.sql.Date(movie.getReleaseDate().getTime()).toLocalDate()
                                : null)
                        .durationMinutes(movie.getDurationMinutes())
                        .ageRating(movie.getAgeRating())
                        .quality(movie.getQuality())
                        .status(movie.getStatus())
                        .genreIds(movie.getGenres().stream().map(Genre::getId).toList())
                        .country(movie.getCountry())
                        .directorNames(movie.getContentCredits() == null ? ""
                                : movie.getContentCredits().stream()
                                        .filter(credit -> credit.getCreditType() == CreditType.DIRECTOR)
                                        .map(credit -> credit.getPerson().getFullName())
                                        .collect(Collectors.joining(", ")))
                        .castNames(movie.getContentCredits() == null ? ""
                                : movie.getContentCredits().stream()
                                        .filter(credit -> credit.getCreditType() == CreditType.CAST)
                                        .map(credit -> credit.getPerson().getFullName())
                                        .collect(Collectors.joining(", ")))
                        .build();

                model.addAttribute("movieUpsertRequest", request);
            }

            populateCurrentAssets(model, movie);
            return "content-manager/movies/form";
        } catch (RuntimeException e) {
            log.error("Không tìm thấy movie để sửa", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/content-manager/movies";
        }
    }

    @PostMapping("/{id}")
    public String updateMovie(
            @PathVariable String id,
            @Valid @ModelAttribute("movieUpsertRequest") MovieUpsertRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateCurrentAssets(model, id);
            return "content-manager/movies/form";
        }

        try {
            movieManagementService.updateMovie(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phim thành công!");
            return "redirect:/content-manager/movies/" + id + "/edit";
        } catch (RuntimeException e) {
            log.error("Lỗi khi update movie {}", id, e);
            bindingResult.reject("error.movie", e.getMessage());
            populateCurrentAssets(model, id);
            return "content-manager/movies/form";
        }
    }

    @PostMapping("/{id}/hide")
    public String hideMovie(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.toggleMovieStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thay đổi trạng thái phim.");
        } catch (RuntimeException e) {
            log.error("Lỗi thay đổi trạng thái phim {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/content-manager/movies";
    }

    @PostMapping("/{id}/restore")
    public String restoreMovie(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.toggleMovieStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thay đổi trạng thái phim.");
        } catch (RuntimeException e) {
            log.error("Lỗi thay đổi trạng thái phim {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/content-manager/movies";
    }

    @PostMapping("/{id}/poster/upload")
    public String uploadMoviePoster(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.uploadMoviePoster(id, file);
            redirectAttributes.addFlashAttribute("successMessage", "Tải lên poster thành công!");
        } catch (IllegalArgumentException e) {
            log.warn("Upload poster thất bại do dữ liệu không hợp lệ cho movie {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (java.io.IOException e) {
            log.error("Upload poster thất bại do lỗi lưu trữ cho movie {}", id, e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Tải lên poster thất bại do lỗi hệ thống lưu trữ.");
        } catch (Exception e) {
            log.error("Upload poster thất bại cho movie {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Tải lên poster thất bại: " + e.getMessage());
        }
        return "redirect:/content-manager/movies/" + id + "/edit";
    }

    @PostMapping("/{id}/trailer/url")
    public String addMovieTrailerFromUrl(
            @PathVariable String id,
            @RequestParam("assetUrl") String assetUrl,
            RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.addMovieTrailerFromUrl(id, assetUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Gắn trailer Cloudinary thành công!");
        } catch (IllegalArgumentException e) {
            log.warn("Gắn trailer thất bại do dữ liệu không hợp lệ cho movie {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Gắn trailer thất bại cho movie {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Gắn trailer thất bại: " + e.getMessage());
        }
        return "redirect:/content-manager/movies/" + id + "/edit";
    }

    @PostMapping("/{id}/video/url")
    public String addMovieVideoFromUrl(
            @PathVariable String id,
            @RequestParam("assetUrl") String assetUrl,
            RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.addMovieVideoFromUrl(id, assetUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Gắn video phim Cloudinary thành công!");
        } catch (IllegalArgumentException e) {
            log.warn("Gắn full video thất bại do dữ liệu không hợp lệ cho movie {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Gắn full video thất bại cho movie {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Gắn video phim thất bại: " + e.getMessage());
        }
        return "redirect:/content-manager/movies/" + id + "/edit";
    }

    @PostMapping("/{movieId}/assets/{assetId}/delete")
    public String deleteMovieAsset(
            @PathVariable String movieId,
            @PathVariable String assetId,
            RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.removeAsset(movieId, assetId);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tài nguyên thành công!");
        } catch (RuntimeException e) {
            log.error("Lỗi khi xóa tài nguyên {} của movie {}", assetId, movieId, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/content-manager/movies/" + movieId + "/edit";
    }

    private void populateCurrentAssets(Model model, String movieId) {
        try {
            populateCurrentAssets(model, movieManagementService.getMovieById(movieId));
        } catch (RuntimeException e) {
            log.warn("Không thể tải tài nguyên hiện tại của movie {}: {}", movieId, e.getMessage());
        }
    }

    private void populateCurrentAssets(Model model, Movie movie) {
        model.addAttribute("currentPosterAsset", findMovieAsset(movie, AssetType.POSTER).orElse(null));
        model.addAttribute("currentTrailerAsset", findMovieAsset(movie, AssetType.TRAILER).orElse(null));
        model.addAttribute("currentVideoAsset", findMovieAsset(movie, AssetType.FULL_VIDEO).orElse(null));
    }

    private Optional<MediaAsset> findMovieAsset(Movie movie, AssetType assetType) {
        if (movie.getMediaAssets() == null) {
            return Optional.empty();
        }

        return movie.getMediaAssets().stream()
                .filter(asset -> asset.getAssetType() == assetType)
                .findFirst();
    }
}
