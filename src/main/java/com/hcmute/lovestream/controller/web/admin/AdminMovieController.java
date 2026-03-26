package com.hcmute.lovestream.controller.web.admin;

import com.hcmute.lovestream.dto.request.admin.movie.MovieUpsertRequest;
import com.hcmute.lovestream.entity.ContentCredit;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.CreditType;
import com.hcmute.lovestream.entity.enums.Quality;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.service.admin.movie.AdminMovieManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/movies")
@RequiredArgsConstructor
@Slf4j
public class AdminMovieController {

    private final AdminMovieManagementService movieManagementService;
    private final GenreRepository genreRepository;

    // --- GLOBAL MODEL ATTRIBUTES ---
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

    // --- ROUTES Thực Thi ---

    // 1. Mở trang Danh Sách
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

        return "admin/movies/list";
    }

    // 2. Mở Form Tạo Mới
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("movieUpsertRequest")) {
            model.addAttribute("movieUpsertRequest", new MovieUpsertRequest());
        }
        return "admin/movies/form";
    }

    // 3. Xử lý lưu Tạo Mới
    @PostMapping
    public String createMovie(
            @Valid @ModelAttribute("movieUpsertRequest") MovieUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "admin/movies/form";
        }

        try {
            Movie created = movieManagementService.createMovie(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Tạo phim thành công! Tiếp tục tải lên poster/trailer/video nếu cần.");
            return "redirect:/admin/movies/" + created.getId() + "/edit";
        } catch (RuntimeException e) {
            log.error("Lỗi khi tạo Movie: ", e);
            bindingResult.reject("error.movie", e.getMessage());
            return "admin/movies/form";
        }
    }

    // 4. Mở Form Cập Nhật
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
                                        .filter(c -> c.getCreditType() == CreditType.DIRECTOR)
                                        .map(c -> c.getPerson().getFullName())
                                        .collect(java.util.stream.Collectors.joining(", ")))
                        .castNames(movie.getContentCredits() == null ? ""
                                : movie.getContentCredits().stream()
                                        .filter(c -> c.getCreditType() == CreditType.CAST)
                                        .map(c -> c.getPerson().getFullName())
                                        .collect(java.util.stream.Collectors.joining(", ")))
                        .build();

                model.addAttribute("movieUpsertRequest", request);
            }
            return "admin/movies/form";
        } catch (RuntimeException e) {
            log.error("Không tìm thấy Movie để sửa: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/movies";
        }
    }

    // 5. Xử lý lưu Cập Nhật
    @PostMapping("/{id}")
    public String updateMovie(
            @PathVariable String id,
            @Valid @ModelAttribute("movieUpsertRequest") MovieUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "admin/movies/form";
        }

        try {
            movieManagementService.updateMovie(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phim thành công!");
            return "redirect:/admin/movies/" + id + "/edit";
        } catch (RuntimeException e) {
            log.error("Lỗi khi update Movie: ", e);
            bindingResult.reject("error.movie", e.getMessage());
            return "admin/movies/form";
        }
    }

    // 6. Action: Ẩn phim nhanh
    @PostMapping("/{id}/hide")
    public String hideMovie(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.toggleMovieStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thay đổi trạng thái phim.");
        } catch (RuntimeException e) {
            log.error("Lỗi thay đổi trạng thái phim: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/movies";
    }

    // 7. Action: Khôi phục phim nhanh
    @PostMapping("/{id}/restore")
    public String restoreMovie(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.toggleMovieStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thay đổi trạng thái phim.");
        } catch (RuntimeException e) {
            log.error("Lỗi thay đổi trạng thái phim: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/movies";
    }

    // 8. Upload poster cho Movie
    @PostMapping("/{id}/poster/upload")
    public String uploadMoviePoster(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.uploadMoviePoster(id, file);
            redirectAttributes.addFlashAttribute("successMessage", "Tải lên poster thành công!");
        } catch (IllegalArgumentException e) {
            log.warn("Upload poster thất bại - dữ liệu không hợp lệ: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (java.io.IOException e) {
            log.error("Upload poster thất bại do lỗi lưu trữ: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Tải lên poster thất bại do lỗi hệ thống lưu trữ.");
        } catch (Exception e) {
            log.error("Upload poster thất bại: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Tải lên poster thất bại: " + e.getMessage());
        }
        return "redirect:/admin/movies/" + id + "/edit";
    }

    // 9. Gắn trailer bằng URL Cloudinary
    @PostMapping("/{id}/trailer/url")
    public String addMovieTrailerFromUrl(
            @PathVariable String id,
            @RequestParam("assetUrl") String assetUrl,
            RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.addMovieTrailerFromUrl(id, assetUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Gắn trailer Cloudinary thành công!");
        } catch (IllegalArgumentException e) {
            log.warn("Gắn trailer thất bại - dữ liệu không hợp lệ: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Gắn trailer thất bại: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Gắn trailer thất bại: " + e.getMessage());
        }
        return "redirect:/admin/movies/" + id + "/edit";
    }

    // 10. Gắn full video bằng URL Cloudinary
    @PostMapping("/{id}/video/url")
    public String addMovieVideoFromUrl(
            @PathVariable String id,
            @RequestParam("assetUrl") String assetUrl,
            RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.addMovieVideoFromUrl(id, assetUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Gắn video phim Cloudinary thành công!");
        } catch (IllegalArgumentException e) {
            log.warn("Gắn full video thất bại - dữ liệu không hợp lệ: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Gắn full video thất bại: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Gắn video phim thất bại: " + e.getMessage());
        }
        return "redirect:/admin/movies/" + id + "/edit";
    }
}
