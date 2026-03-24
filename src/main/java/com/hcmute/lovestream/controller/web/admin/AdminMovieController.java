package com.hcmute.lovestream.controller.web.admin;

import com.hcmute.lovestream.dto.request.admin.movie.MovieUpsertRequest;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.Quality;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.service.admin.movie.AdminMovieManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final GenreRepository genreRepository; // Tiêm vào để load Option cho UI thẻ Select

    // --- GLOBAL MODEL ATTRIBUTES ---
    // Các Helper Method này sẽ được gọi tự động để nạp Data vào Model
    // (nhờ đó không cần gõ Model.addAttribute nhiều lần ở các hàm Render Form)
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

    // 1. Mở trang Danh Sách (Gồm Lọc + Core List)
    @GetMapping
    public String listMovies(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Page<Movie> moviePage = movieManagementService.filterMovies(
                status,
                keyword,
                PageRequest.of(safePage, safeSize)
        );

        model.addAttribute("movies", moviePage.getContent());
        model.addAttribute("moviePage", moviePage);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("pageSize", safeSize);
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
            // Lấy thông tin phim cũ
            Movie movie = movieManagementService.getMovieById(id);
            
            // Map thủ công thông tin entity sang DTO cho view 
            // Nếu model chưa mang theo movieUpsertRequest do redirect fail
            if (!model.containsAttribute("movieUpsertRequest")) {
                MovieUpsertRequest request = MovieUpsertRequest.builder()
                        .id(movie.getId())
                        .title(movie.getTitle())
                        .description(movie.getDescription())
                        .releaseYear(movie.getReleaseYear())
                        .releaseDate(movie.getReleaseDate() != null ? new java.sql.Date(movie.getReleaseDate().getTime()).toLocalDate() : null)
                        .durationMinutes(movie.getDurationMinutes())
                        .ageRating(movie.getAgeRating())
                        .quality(movie.getQuality())
                        .status(movie.getStatus())
                        // Convert cấu trúc Set<Genre> thành List<String> 
                        .genreIds(movie.getGenres().stream().map(Genre::getId).toList())
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
            movieManagementService.hideMovie(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ẩn phim.");
        } catch (RuntimeException e) {
            log.error("Lỗi ẩn phim: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/movies";
    }

    @ModelAttribute("allAssetTypes")
    public AssetType[] populateAssetTypes() {
        // Only offer asset types relevant to a Movie (exclude EPISODE_VIDEO)
        return new AssetType[]{AssetType.POSTER, AssetType.BACKGROUND, AssetType.TRAILER, AssetType.MOVIE_VIDEO};
    }

    // 7. Action: Khôi phục phim nhanh
    @PostMapping("/{id}/restore")
    public String restoreMovie(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.restoreMovie(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã khôi phục trạng thái phim.");
        } catch (RuntimeException e) {
            log.error("Lỗi khôi phục phim: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/movies";
    }

    // 8. Upload Media Asset cho Movie
    @PostMapping("/{id}/assets")
    public String uploadMovieAsset(
            @PathVariable String id,
            @RequestParam("assetType") AssetType assetType,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            movieManagementService.uploadMovieAsset(id, assetType, file);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Upload tài nguyên " + assetType.name() + " thành công!");
        } catch (IllegalArgumentException e) {
            log.warn("Upload thất bại - dữ liệu không hợp lệ: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Upload thất bại: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Upload thất bại: " + e.getMessage());
        }
        return "redirect:/admin/movies/" + id + "/edit";
    }
}

