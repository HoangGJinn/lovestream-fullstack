package com.hcmute.lovestream.controller.web.contentmanager;

import com.hcmute.lovestream.dto.request.contentmanager.series.EpisodeUpsertRequest;
import com.hcmute.lovestream.dto.request.contentmanager.series.SeasonUpsertRequest;
import com.hcmute.lovestream.dto.request.contentmanager.series.TVSeriesUpsertRequest;
import com.hcmute.lovestream.entity.ContentCredit;
import com.hcmute.lovestream.entity.Episode;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.Season;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.CreditType;
import com.hcmute.lovestream.entity.enums.Quality;
import com.hcmute.lovestream.repository.EpisodeRepository;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.repository.SeasonRepository;
import com.hcmute.lovestream.service.contentmanager.series.ContentManagerSeriesManagementService;
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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/content-manager/series")
@RequiredArgsConstructor
@Slf4j
public class ContentManagerSeriesWebController {

    private static final String SERIES_POSTER_UPLOAD_ERROR_MESSAGE = "seriesPosterUploadErrorMessage";
    private static final String SERIES_POSTER_UPLOAD_SUCCESS_MESSAGE = "seriesPosterUploadSuccessMessage";

    private final ContentManagerSeriesManagementService seriesService;
    private final GenreRepository genreRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

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
    public String listSeries(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<TVSeries> seriesPage = seriesService.getSeries(keyword, status, pageable);

        model.addAttribute("seriesList", seriesPage.getContent());
        model.addAttribute("seriesPage", seriesPage);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("totalPages", seriesPage.getTotalPages());
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentKeyword", keyword);
        return "content-manager/series/list";
    }

    @GetMapping("/new")
    public String showCreateSeriesForm(Model model) {
        if (!model.containsAttribute("tvSeriesUpsertRequest")) {
            model.addAttribute("tvSeriesUpsertRequest", new TVSeriesUpsertRequest());
        }
        return "content-manager/series/form";
    }

    @PostMapping
    public String createSeries(
            @Valid @ModelAttribute("tvSeriesUpsertRequest") TVSeriesUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "content-manager/series/form";
        }

        try {
            TVSeries created = seriesService.createSeries(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Tạo TV Series thành công! Tiếp tục thêm poster/trailer nếu cần.");
            return "redirect:/content-manager/series/" + created.getId() + "/edit";
        } catch (RuntimeException e) {
            log.error("Lỗi khi tạo TV Series", e);
            bindingResult.reject("error.series", e.getMessage());
            return "content-manager/series/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditSeriesForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            TVSeries series = seriesService.getSeriesById(id);
            if (!model.containsAttribute("tvSeriesUpsertRequest")) {
                model.addAttribute("tvSeriesUpsertRequest", buildSeriesUpsertRequest(series));
            }
            populateSeriesEditModel(model, series);
            return "content-manager/series/form";
        } catch (RuntimeException e) {
            log.error("Không tìm thấy Series để sửa", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/content-manager/series";
        }
    }

    @PostMapping("/{id}")
    public String updateSeries(
            @PathVariable String id,
            @Valid @ModelAttribute("tvSeriesUpsertRequest") TVSeriesUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            populateSeriesEditModel(model, seriesService.getSeriesById(id));
            return "content-manager/series/form";
        }

        try {
            seriesService.updateSeries(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật TV Series thành công!");
            return "redirect:/content-manager/series/" + id + "/edit";
        } catch (RuntimeException e) {
            log.error("Lỗi khi update TV Series", e);
            bindingResult.reject("error.series", e.getMessage());
            populateSeriesEditModel(model, seriesService.getSeriesById(id));
            return "content-manager/series/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleSeriesStatus(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            seriesService.toggleSeriesStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thay đổi trạng thái series.");
        } catch (RuntimeException e) {
            log.error("Lỗi toggle status series", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/content-manager/series";
    }

    @PostMapping("/{id}/delete")
    public String deleteSeries(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            seriesService.deleteSeries(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa TV Series và toàn bộ mùa/tập bên trong.");
        } catch (RuntimeException e) {
            log.error("Lỗi xóa series", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/content-manager/series";
    }

    @PostMapping("/{id}/trailer/url")
    public String addSeriesTrailerFromUrl(
            @PathVariable String id,
            @RequestParam("assetUrl") String assetUrl,
            RedirectAttributes redirectAttributes) {
        try {
            seriesService.addSeriesTrailerFromUrl(id, assetUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu URL trailer thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi gắn trailer series", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Gắn trailer thất bại: " + e.getMessage());
        }
        return "redirect:/content-manager/series/" + id + "/edit";
    }

    @PostMapping("/{id}/poster/upload")
    public String uploadSeriesPoster(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            seriesService.uploadSeriesPoster(id, file);
            redirectAttributes.addFlashAttribute(SERIES_POSTER_UPLOAD_SUCCESS_MESSAGE, "Tải lên poster thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(SERIES_POSTER_UPLOAD_ERROR_MESSAGE, e.getMessage());
        } catch (IOException e) {
            log.error("Lỗi upload poster series", e);
            redirectAttributes.addFlashAttribute(
                    SERIES_POSTER_UPLOAD_ERROR_MESSAGE,
                    "Tải lên thất bại do lỗi hệ thống lưu trữ.");
        } catch (Exception e) {
            log.error("Lỗi khi upload poster series", e);
            redirectAttributes.addFlashAttribute(
                    SERIES_POSTER_UPLOAD_ERROR_MESSAGE,
                    "Tải lên thất bại: " + e.getMessage());
        }
        return "redirect:/content-manager/series/" + id + "/edit";
    }

    @GetMapping("/{seriesId}/seasons/new")
    public String showCreateSeasonForm(@PathVariable String seriesId, Model model, RedirectAttributes redirectAttributes) {
        try {
            TVSeries series = seriesService.getSeriesById(seriesId);
            model.addAttribute("series", series);
            if (!model.containsAttribute("seasonUpsertRequest")) {
                SeasonUpsertRequest req = new SeasonUpsertRequest();
                req.setTvSeriesId(seriesId);
                model.addAttribute("seasonUpsertRequest", req);
            }
            return "content-manager/series/season_form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/content-manager/series";
        }
    }

    @PostMapping("/{seriesId}/seasons")
    public String createSeason(
            @PathVariable String seriesId,
            @Valid @ModelAttribute("seasonUpsertRequest") SeasonUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        request.setTvSeriesId(seriesId);
        if (bindingResult.hasErrors()) {
            model.addAttribute("series", seriesService.getSeriesById(seriesId));
            return "content-manager/series/season_form";
        }

        try {
            Season created = seriesService.createSeason(request);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo mùa thành công!");
            return "redirect:/content-manager/series/seasons/" + created.getId() + "/edit";
        } catch (RuntimeException e) {
            log.error("Lỗi tạo Season", e);
            bindingResult.reject("error.season", e.getMessage());
            model.addAttribute("series", seriesService.getSeriesById(seriesId));
            return "content-manager/series/season_form";
        }
    }

    @GetMapping("/seasons/{seasonId}/edit")
    public String showEditSeasonForm(@PathVariable String seasonId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Season season = seriesService.getSeasonById(seasonId);

            if (!model.containsAttribute("seasonUpsertRequest")) {
                SeasonUpsertRequest req = SeasonUpsertRequest.builder()
                        .id(season.getId())
                        .tvSeriesId(season.getTvSeries().getId())
                        .seasonNumber(season.getSeasonNumber())
                        .name(season.getName())
                        .releaseYear(season.getReleaseYear())
                        .build();
                model.addAttribute("seasonUpsertRequest", req);
            }

            model.addAttribute("season", season);
            model.addAttribute("series", season.getTvSeries());
            model.addAttribute("episodes", episodeRepository.findBySeasonOrderByEpisodeNumberAsc(season));

            return "content-manager/series/season_form";
        } catch (RuntimeException e) {
            log.error("Không tìm thấy Season", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/content-manager/series";
        }
    }

    @PostMapping("/seasons/{seasonId}")
    public String updateSeason(
            @PathVariable String seasonId,
            @Valid @ModelAttribute("seasonUpsertRequest") SeasonUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            Season season = seriesService.getSeasonById(seasonId);
            model.addAttribute("season", season);
            model.addAttribute("series", season.getTvSeries());
            model.addAttribute("episodes", episodeRepository.findBySeasonOrderByEpisodeNumberAsc(season));
            return "content-manager/series/season_form";
        }

        try {
            seriesService.updateSeason(seasonId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật mùa thành công!");
            return "redirect:/content-manager/series/seasons/" + seasonId + "/edit";
        } catch (RuntimeException e) {
            log.error("Lỗi update Season", e);
            bindingResult.reject("error.season", e.getMessage());
            Season season = seriesService.getSeasonById(seasonId);
            model.addAttribute("season", season);
            model.addAttribute("series", season.getTvSeries());
            model.addAttribute("episodes", episodeRepository.findBySeasonOrderByEpisodeNumberAsc(season));
            return "content-manager/series/season_form";
        }
    }

    @PostMapping("/seasons/{seasonId}/delete")
    public String deleteSeason(@PathVariable String seasonId, RedirectAttributes redirectAttributes) {
        String seriesId;
        try {
            Season season = seriesService.getSeasonById(seasonId);
            seriesId = season.getTvSeries().getId();
            seriesService.deleteSeason(seasonId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa mùa và toàn bộ tập bên trong.");
        } catch (RuntimeException e) {
            log.error("Lỗi xóa Season", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/content-manager/series";
        }
        return "redirect:/content-manager/series/" + seriesId + "/edit";
    }

    @GetMapping("/seasons/{seasonId}/episodes/new")
    public String showCreateEpisodeForm(@PathVariable String seasonId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Season season = seriesService.getSeasonById(seasonId);
            model.addAttribute("season", season);
            model.addAttribute("series", season.getTvSeries());
            if (!model.containsAttribute("episodeUpsertRequest")) {
                EpisodeUpsertRequest req = new EpisodeUpsertRequest();
                req.setSeasonId(seasonId);
                model.addAttribute("episodeUpsertRequest", req);
            }
            return "content-manager/series/episode_form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/content-manager/series";
        }
    }

    @PostMapping("/seasons/{seasonId}/episodes")
    public String createEpisode(
            @PathVariable String seasonId,
            @Valid @ModelAttribute("episodeUpsertRequest") EpisodeUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        request.setSeasonId(seasonId);
        if (bindingResult.hasErrors()) {
            Season season = seriesService.getSeasonById(seasonId);
            model.addAttribute("season", season);
            model.addAttribute("series", season.getTvSeries());
            return "content-manager/series/episode_form";
        }

        try {
            Episode created = seriesService.createEpisode(request);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tập phim thành công!");
            return "redirect:/content-manager/series/episodes/" + created.getId() + "/edit";
        } catch (RuntimeException e) {
            log.error("Lỗi tạo Episode", e);
            bindingResult.reject("error.episode", e.getMessage());
            Season season = seriesService.getSeasonById(seasonId);
            model.addAttribute("season", season);
            model.addAttribute("series", season.getTvSeries());
            return "content-manager/series/episode_form";
        }
    }

    @GetMapping("/episodes/{episodeId}/edit")
    public String showEditEpisodeForm(@PathVariable String episodeId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Episode episode = seriesService.getEpisodeById(episodeId);

            if (!model.containsAttribute("episodeUpsertRequest")) {
                EpisodeUpsertRequest req = EpisodeUpsertRequest.builder()
                        .id(episode.getId())
                        .seasonId(episode.getSeason().getId())
                        .episodeNumber(episode.getEpisodeNumber())
                        .title(episode.getTitle())
                        .durationInMinutes(episode.getDurationInMinutes())
                        .airDate(episode.getAirDate() != null
                                ? new java.sql.Date(episode.getAirDate().getTime()).toLocalDate()
                                : null)
                        .build();
                model.addAttribute("episodeUpsertRequest", req);
            }

            model.addAttribute("episode", episode);
            model.addAttribute("season", episode.getSeason());
            model.addAttribute("series", episode.getSeason().getTvSeries());

            return "content-manager/series/episode_form";
        } catch (RuntimeException e) {
            log.error("Không tìm thấy Episode", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/content-manager/series";
        }
    }

    @PostMapping("/episodes/{episodeId}")
    public String updateEpisode(
            @PathVariable String episodeId,
            @Valid @ModelAttribute("episodeUpsertRequest") EpisodeUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            Episode ep = seriesService.getEpisodeById(episodeId);
            model.addAttribute("episode", ep);
            model.addAttribute("season", ep.getSeason());
            model.addAttribute("series", ep.getSeason().getTvSeries());
            return "content-manager/series/episode_form";
        }

        try {
            seriesService.updateEpisode(episodeId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tập phim thành công!");
            return "redirect:/content-manager/series/episodes/" + episodeId + "/edit";
        } catch (RuntimeException e) {
            log.error("Lỗi update Episode", e);
            bindingResult.reject("error.episode", e.getMessage());
            Episode ep = seriesService.getEpisodeById(episodeId);
            model.addAttribute("episode", ep);
            model.addAttribute("season", ep.getSeason());
            model.addAttribute("series", ep.getSeason().getTvSeries());
            return "content-manager/series/episode_form";
        }
    }

    @PostMapping("/episodes/{episodeId}/delete")
    public String deleteEpisode(@PathVariable String episodeId, RedirectAttributes redirectAttributes) {
        String seasonId;
        try {
            Episode episode = seriesService.getEpisodeById(episodeId);
            seasonId = episode.getSeason().getId();
            seriesService.deleteEpisode(episodeId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tập phim.");
        } catch (RuntimeException e) {
            log.error("Lỗi xóa Episode", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/content-manager/series";
        }
        return "redirect:/content-manager/series/seasons/" + seasonId + "/edit";
    }

    @PostMapping("/episodes/{episodeId}/video/url")
    public String addEpisodeVideoFromUrl(
            @PathVariable String episodeId,
            @RequestParam("assetUrl") String assetUrl,
            RedirectAttributes redirectAttributes) {
        try {
            seriesService.addEpisodeVideoFromUrl(episodeId, assetUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Gắn video tập phim thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi gắn video tập phim", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Gắn video thất bại: " + e.getMessage());
        }
        return "redirect:/content-manager/series/episodes/" + episodeId + "/edit";
    }

    private TVSeriesUpsertRequest buildSeriesUpsertRequest(TVSeries series) {
        List<ContentCredit> credits = series.getContentCredits();
        String directorNames = credits == null ? "" : credits.stream()
                .filter(c -> c.getCreditType() == CreditType.DIRECTOR)
                .map(c -> c.getPerson().getFullName())
                .collect(Collectors.joining(", "));
        String castNames = credits == null ? "" : credits.stream()
                .filter(c -> c.getCreditType() == CreditType.CAST)
                .map(c -> c.getPerson().getFullName())
                .collect(Collectors.joining(", "));

        return TVSeriesUpsertRequest.builder()
                .id(series.getId())
                .title(series.getTitle())
                .description(series.getDescription())
                .releaseYear(series.getReleaseYear())
                .ageRating(series.getAgeRating())
                .quality(series.getQuality())
                .status(series.getStatus())
                .durationMinutes(series.getDurationMinutes())
                .genreIds(series.getGenres().stream().map(Genre::getId).collect(Collectors.toList()))
                .directorNames(directorNames)
                .castNames(castNames)
                .build();
    }

    private void populateSeriesEditModel(Model model, TVSeries series) {
        model.addAttribute("seasons", seasonRepository.findByTvSeriesOrderBySeasonNumberAsc(series));
        model.addAttribute("series", series);
    }
}
