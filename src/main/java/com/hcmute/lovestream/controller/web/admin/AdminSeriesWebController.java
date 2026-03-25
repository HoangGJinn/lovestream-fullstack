package com.hcmute.lovestream.controller.web.admin;

import com.hcmute.lovestream.dto.request.admin.series.EpisodeUpsertRequest;
import com.hcmute.lovestream.dto.request.admin.series.SeasonUpsertRequest;
import com.hcmute.lovestream.dto.request.admin.series.TVSeriesUpsertRequest;
import com.hcmute.lovestream.entity.ContentCredit;
import com.hcmute.lovestream.entity.Episode;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.Season;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.CreditType;
import com.hcmute.lovestream.entity.enums.Quality;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.repository.SeasonRepository;
import com.hcmute.lovestream.repository.EpisodeRepository;
import com.hcmute.lovestream.service.admin.series.AdminSeriesManagementService;
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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/series")
@RequiredArgsConstructor
@Slf4j
public class AdminSeriesWebController {

    private final AdminSeriesManagementService seriesService;
    private final GenreRepository genreRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    // ---- Global model attributes ----

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

    @ModelAttribute("seriesAssetTypes")
    public AssetType[] populateSeriesAssetTypes() {
        return new AssetType[]{AssetType.POSTER, AssetType.TRAILER};
    }

    // =====================================================================
    //  TV Series endpoints
    // =====================================================================

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
        return "admin/series/list";
    }

    @GetMapping("/new")
    public String showCreateSeriesForm(Model model) {
        if (!model.containsAttribute("tvSeriesUpsertRequest")) {
            model.addAttribute("tvSeriesUpsertRequest", new TVSeriesUpsertRequest());
        }
        return "admin/series/form";
    }

    @PostMapping
    public String createSeries(
            @Valid @ModelAttribute("tvSeriesUpsertRequest") TVSeriesUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "admin/series/form";
        }

        try {
            TVSeries created = seriesService.createSeries(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "T\u1EA1o TV Series th\u00e0nh c\u00f4ng! Ti\u1EBFp t\u1EE5c th\u00eam poster/trailer n\u1EBFu c\u1EA7n.");
            return "redirect:/admin/series/" + created.getId() + "/edit";
        } catch (RuntimeException e) {
            log.error("L\u1ED7i khi t\u1EA1o TV Series: ", e);
            bindingResult.reject("error.series", e.getMessage());
            return "admin/series/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditSeriesForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            TVSeries series = seriesService.getSeriesById(id);

            if (!model.containsAttribute("tvSeriesUpsertRequest")) {
                // Extract director/cast names for the form
                List<ContentCredit> credits = series.getContentCredits();
                String directorNames = credits == null ? "" : credits.stream()
                        .filter(c -> c.getCreditType() == CreditType.DIRECTOR)
                        .map(c -> c.getPerson().getFullName())
                        .collect(Collectors.joining(", "));
                String castNames = credits == null ? "" : credits.stream()
                        .filter(c -> c.getCreditType() == CreditType.CAST)
                        .map(c -> c.getPerson().getFullName())
                        .collect(Collectors.joining(", "));

                TVSeriesUpsertRequest req = TVSeriesUpsertRequest.builder()
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

                model.addAttribute("tvSeriesUpsertRequest", req);
            }

            // Pass seasons list for this series
            List<Season> seasons = seasonRepository.findByTvSeriesOrderBySeasonNumberAsc(series);
            model.addAttribute("seasons", seasons);
            model.addAttribute("series", series);

            return "admin/series/form";
        } catch (RuntimeException e) {
            log.error("Kh\u00f4ng t\u00ecm th\u1EA5y Series \u0111\u1EC3 s\u1EEDa: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/series";
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
            TVSeries series = seriesService.getSeriesById(id);
            List<Season> seasons = seasonRepository.findByTvSeriesOrderBySeasonNumberAsc(series);
            model.addAttribute("seasons", seasons);
            model.addAttribute("series", series);
            return "admin/series/form";
        }

        try {
            seriesService.updateSeries(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "C\u1EADp nh\u1EADt TV Series th\u00e0nh c\u00f4ng!");
            return "redirect:/admin/series/" + id + "/edit";
        } catch (RuntimeException e) {
            log.error("L\u1ED7i khi update TV Series: ", e);
            bindingResult.reject("error.series", e.getMessage());
            TVSeries series = seriesService.getSeriesById(id);
            List<Season> seasons = seasonRepository.findByTvSeriesOrderBySeasonNumberAsc(series);
            model.addAttribute("seasons", seasons);
            model.addAttribute("series", series);
            return "admin/series/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleSeriesStatus(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            seriesService.toggleSeriesStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "\u0110\u00e3 thay \u0111\u1ED5i tr\u1EA1ng th\u00e1i series.");
        } catch (RuntimeException e) {
            log.error("L\u1ED7i toggle status series: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/series";
    }

    @PostMapping("/{id}/delete")
    public String deleteSeries(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            seriesService.deleteSeries(id);
            redirectAttributes.addFlashAttribute("successMessage", "\u0110\u00e3 x\u00f3a TV Series v\u00e0 to\u00e0n b\u1ED9 m\u00f9a/t\u1EADp b\u00ean trong.");
        } catch (RuntimeException e) {
            log.error("L\u1ED7i x\u00f3a series: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/series";
    }

    // Asset endpoints for series
    @PostMapping("/{id}/assets/url")
    public String addSeriesAssetFromUrl(
            @PathVariable String id,
            @RequestParam("assetType") AssetType assetType,
            @RequestParam("assetUrl") String assetUrl,
            RedirectAttributes redirectAttributes) {
        try {
            seriesService.addSeriesAssetFromUrl(id, assetType, assetUrl);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Th\u00eam t\u00e0i nguy\u00ean " + assetType.name() + " th\u00e0nh c\u00f4ng!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("L\u1ED7i th\u00eam asset series: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Th\u00eam th\u1EA5t b\u1EA1i: " + e.getMessage());
        }
        return "redirect:/admin/series/" + id + "/edit";
    }

    @PostMapping("/{id}/assets/upload")
    public String uploadSeriesAsset(
            @PathVariable String id,
            @RequestParam("assetType") AssetType assetType,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            seriesService.uploadSeriesAsset(id, assetType, file);
            redirectAttributes.addFlashAttribute("successMessage",
                    "T\u1EA3i l\u00ean t\u00e0i nguy\u00ean " + assetType.name() + " th\u00e0nh c\u00f4ng!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IOException e) {
            log.error("L\u1ED7i upload asset series: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "T\u1EA3i l\u00ean th\u1EA5t b\u1EA1i do l\u1ED7i h\u1EC7 th\u1ED1ng l\u01B0u tr\u1EEF.");
        } catch (Exception e) {
            log.error("L\u1ED7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh khi upload asset series: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "T\u1EA3i l\u00ean th\u1EA5t b\u1EA1i: " + e.getMessage());
        }
        return "redirect:/admin/series/" + id + "/edit";
    }

    // =====================================================================
    //  Season endpoints
    // =====================================================================

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
            return "admin/series/season_form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/series";
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
            return "admin/series/season_form";
        }

        try {
            Season created = seriesService.createSeason(request);
            redirectAttributes.addFlashAttribute("successMessage", "T\u1EA1o m\u00f9a th\u00e0nh c\u00f4ng!");
            return "redirect:/admin/series/seasons/" + created.getId() + "/edit";
        } catch (RuntimeException e) {
            log.error("L\u1ED7i t\u1EA1o Season: ", e);
            bindingResult.reject("error.season", e.getMessage());
            model.addAttribute("series", seriesService.getSeriesById(seriesId));
            return "admin/series/season_form";
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

            List<Episode> episodes = episodeRepository.findBySeasonOrderByEpisodeNumberAsc(season);
            model.addAttribute("episodes", episodes);

            return "admin/series/season_form";
        } catch (RuntimeException e) {
            log.error("Kh\u00f4ng t\u00ecm th\u1EA5y Season: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/series";
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
            return "admin/series/season_form";
        }

        try {
            seriesService.updateSeason(seasonId, request);
            redirectAttributes.addFlashAttribute("successMessage", "C\u1EADp nh\u1EADt m\u00f9a th\u00e0nh c\u00f4ng!");
            return "redirect:/admin/series/seasons/" + seasonId + "/edit";
        } catch (RuntimeException e) {
            log.error("L\u1ED7i update Season: ", e);
            bindingResult.reject("error.season", e.getMessage());
            Season season = seriesService.getSeasonById(seasonId);
            model.addAttribute("season", season);
            model.addAttribute("series", season.getTvSeries());
            model.addAttribute("episodes", episodeRepository.findBySeasonOrderByEpisodeNumberAsc(season));
            return "admin/series/season_form";
        }
    }

    @PostMapping("/seasons/{seasonId}/delete")
    public String deleteSeason(@PathVariable String seasonId, RedirectAttributes redirectAttributes) {
        String seriesId;
        try {
            Season season = seriesService.getSeasonById(seasonId);
            seriesId = season.getTvSeries().getId();
            seriesService.deleteSeason(seasonId);
            redirectAttributes.addFlashAttribute("successMessage", "\u0110\u00e3 x\u00f3a m\u00f9a v\u00e0 to\u00e0n b\u1ED9 t\u1EADp b\u00ean trong.");
        } catch (RuntimeException e) {
            log.error("L\u1ED7i x\u00f3a Season: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/series";
        }
        return "redirect:/admin/series/" + seriesId + "/edit";
    }

    @PostMapping("/seasons/{seasonId}/assets/url")
    public String addSeasonAssetFromUrl(
            @PathVariable String seasonId,
            @RequestParam("assetUrl") String assetUrl,
            RedirectAttributes redirectAttributes) {
        try {
            seriesService.addSeasonAssetFromUrl(seasonId, assetUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Th\u00eam poster m\u00f9a th\u00e0nh c\u00f4ng!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("L\u1ED7i th\u00eam asset season: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Th\u00eam th\u1EA5t b\u1EA1i: " + e.getMessage());
        }
        return "redirect:/admin/series/seasons/" + seasonId + "/edit";
    }

    @PostMapping("/seasons/{seasonId}/assets/upload")
    public String uploadSeasonAsset(
            @PathVariable String seasonId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            seriesService.uploadSeasonAsset(seasonId, file);
            redirectAttributes.addFlashAttribute("successMessage", "T\u1EA3i l\u00ean poster m\u00f9a th\u00e0nh c\u00f4ng!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IOException e) {
            log.error("L\u1ED7i upload asset season: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "T\u1EA3i l\u00ean th\u1EA5t b\u1EA1i do l\u1ED7i h\u1EC7 th\u1ED1ng l\u01B0u tr\u1EEF.");
        } catch (Exception e) {
            log.error("L\u1ED7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh khi upload asset season: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "T\u1EA3i l\u00ean th\u1EA5t b\u1EA1i: " + e.getMessage());
        }
        return "redirect:/admin/series/seasons/" + seasonId + "/edit";
    }

    // =====================================================================
    //  Episode endpoints
    // =====================================================================

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
            return "admin/series/episode_form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/series";
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
            return "admin/series/episode_form";
        }

        try {
            Episode created = seriesService.createEpisode(request);
            redirectAttributes.addFlashAttribute("successMessage", "T\u1EA1o t\u1EADp phim th\u00e0nh c\u00f4ng!");
            return "redirect:/admin/series/episodes/" + created.getId() + "/edit";
        } catch (RuntimeException e) {
            log.error("L\u1ED7i t\u1EA1o Episode: ", e);
            bindingResult.reject("error.episode", e.getMessage());
            Season season = seriesService.getSeasonById(seasonId);
            model.addAttribute("season", season);
            model.addAttribute("series", season.getTvSeries());
            return "admin/series/episode_form";
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

            return "admin/series/episode_form";
        } catch (RuntimeException e) {
            log.error("Kh\u00f4ng t\u00ecm th\u1EA5y Episode: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/series";
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
            return "admin/series/episode_form";
        }

        try {
            seriesService.updateEpisode(episodeId, request);
            redirectAttributes.addFlashAttribute("successMessage", "C\u1EADp nh\u1EADt t\u1EADp phim th\u00e0nh c\u00f4ng!");
            return "redirect:/admin/series/episodes/" + episodeId + "/edit";
        } catch (RuntimeException e) {
            log.error("L\u1ED7i update Episode: ", e);
            bindingResult.reject("error.episode", e.getMessage());
            Episode ep = seriesService.getEpisodeById(episodeId);
            model.addAttribute("episode", ep);
            model.addAttribute("season", ep.getSeason());
            model.addAttribute("series", ep.getSeason().getTvSeries());
            return "admin/series/episode_form";
        }
    }

    @PostMapping("/episodes/{episodeId}/delete")
    public String deleteEpisode(@PathVariable String episodeId, RedirectAttributes redirectAttributes) {
        String seasonId;
        try {
            Episode episode = seriesService.getEpisodeById(episodeId);
            seasonId = episode.getSeason().getId();
            seriesService.deleteEpisode(episodeId);
            redirectAttributes.addFlashAttribute("successMessage", "\u0110\u00e3 x\u00f3a t\u1EADp phim.");
        } catch (RuntimeException e) {
            log.error("L\u1ED7i x\u00f3a Episode: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/series";
        }
        return "redirect:/admin/series/seasons/" + seasonId + "/edit";
    }

    @PostMapping("/episodes/{episodeId}/assets/url")
    public String addEpisodeAssetFromUrl(
            @PathVariable String episodeId,
            @RequestParam("assetUrl") String assetUrl,
            RedirectAttributes redirectAttributes) {
        try {
            seriesService.addEpisodeAssetFromUrl(episodeId, assetUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Th\u00eam video t\u1EADp phim th\u00e0nh c\u00f4ng!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("L\u1ED7i th\u00eam asset episode: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Th\u00eam th\u1EA5t b\u1EA1i: " + e.getMessage());
        }
        return "redirect:/admin/series/episodes/" + episodeId + "/edit";
    }

    @PostMapping("/episodes/{episodeId}/assets/upload")
    public String uploadEpisodeAsset(
            @PathVariable String episodeId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            seriesService.uploadEpisodeAsset(episodeId, file);
            redirectAttributes.addFlashAttribute("successMessage", "T\u1EA3i l\u00ean video t\u1EADp phim th\u00e0nh c\u00f4ng!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IOException e) {
            log.error("L\u1ED7i upload asset episode: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "T\u1EA3i l\u00ean th\u1EA5t b\u1EA1i do l\u1ED7i h\u1EC7 th\u1ED1ng l\u01B0u tr\u1EEF.");
        } catch (Exception e) {
            log.error("L\u1ED7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh khi upload asset episode: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "T\u1EA3i l\u00ean th\u1EA5t b\u1EA1i: " + e.getMessage());
        }
        return "redirect:/admin/series/episodes/" + episodeId + "/edit";
    }
}
