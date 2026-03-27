package com.hcmute.lovestream.controller.web.contentmanager;

import com.hcmute.lovestream.dto.request.webcontent.WebContentBannerReorderRequest;
import com.hcmute.lovestream.dto.request.webcontent.WebContentBannerUpsertRequest;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.WebContentBanner;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.WebContentBannerTargetType;
import com.hcmute.lovestream.entity.enums.WebStaticPageType;
import com.hcmute.lovestream.repository.MovieRepository;
import com.hcmute.lovestream.repository.TVSeriesRepository;
import com.hcmute.lovestream.service.webcontent.WebContentBannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/content-manager/web-content/banners")
@RequiredArgsConstructor
@Slf4j
public class ContentManagerWebContentBannerController {

    private final WebContentBannerService bannerService;
    private final MovieRepository movieRepository;
    private final TVSeriesRepository tvSeriesRepository;

    @ModelAttribute("bannerTargetTypes")
    public WebContentBannerTargetType[] populateTargetTypes() {
        return WebContentBannerTargetType.values();
    }

    @ModelAttribute("availableMovies")
    public List<SelectOption> populateMovies() {
        return movieRepository.findAllByOrderByTitleAsc().stream()
                .map(movie -> new SelectOption(movie.getId(), buildContentLabel(movie.getTitle(), movie.getStatus())))
                .toList();
    }

    @ModelAttribute("availableSeries")
    public List<SelectOption> populateSeries() {
        return tvSeriesRepository.findAll().stream()
                .sorted(Comparator.comparing((TVSeries series) -> sortKey(series.getTitle()), String.CASE_INSENSITIVE_ORDER))
                .map(series -> new SelectOption(series.getId(), buildContentLabel(series.getTitle(), series.getStatus())))
                .toList();
    }

    @ModelAttribute("availableStaticPages")
    public List<SelectOption> populateStaticPages() {
        return List.of(
                new SelectOption(WebStaticPageType.ABOUT.name(), "Giới thiệu (/about)"),
                new SelectOption(WebStaticPageType.PRIVACY_POLICY.name(), "Chính sách bảo mật (/privacy-policy)"),
                new SelectOption(WebStaticPageType.TERMS.name(), "Điều khoản sử dụng (/terms)")
        );
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        int nextOrder = bannerService.getAllOrdered().size() + 1;
        WebContentBannerUpsertRequest request = WebContentBannerUpsertRequest.builder()
                .displayOrder(nextOrder)
                .isDisplayed(true)
                .targetType(WebContentBannerTargetType.NONE)
                .build();
        model.addAttribute("bannerRequest", request);
        return "content-manager/web-content/banners/form";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            WebContentBanner banner = bannerService.getOrThrow(id);
            WebContentBannerUpsertRequest request = WebContentBannerUpsertRequest.builder()
                    .id(banner.getId())
                    .title(banner.getTitle())
                    .displayOrder(banner.getDisplayOrder())
                    .isDisplayed(banner.getIsDisplayed())
                    .existingImagePath(banner.getImagePath())
                    .build();
            populateNavigationTargetFields(request, banner);
            model.addAttribute("bannerRequest", request);
            return "content-manager/web-content/banners/form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/content-manager/web-content?tab=banners";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("bannerRequest") WebContentBannerUpsertRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "content-manager/web-content/banners/form";
        }

        try {
            bannerService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo banner thành công!");
            return "redirect:/content-manager/web-content?tab=banners";
        } catch (IllegalArgumentException e) {
            log.warn("Create banner failed: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "content-manager/web-content/banners/form";
        }
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("bannerRequest") WebContentBannerUpsertRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            WebContentBanner existing = bannerService.getOrThrow(id);
            request.setExistingImagePath(existing.getImagePath());
            return "content-manager/web-content/banners/form";
        }

        try {
            bannerService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật banner thành công!");
            return "redirect:/content-manager/web-content?tab=banners";
        } catch (IllegalArgumentException e) {
            log.warn("Update banner failed: {}", e.getMessage());
            WebContentBanner existing = bannerService.getOrThrow(id);
            request.setExistingImagePath(existing.getImagePath());
            model.addAttribute("errorMessage", e.getMessage());
            return "content-manager/web-content/banners/form";
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggleDisplayed(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bannerService.toggleDisplayed(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái hiển thị banner.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/content-manager/web-content?tab=banners";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bannerService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa banner thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/content-manager/web-content?tab=banners";
    }

    @PostMapping(value = "/reorder", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reorder(@RequestBody WebContentBannerReorderRequest request) {
        bannerService.reorder(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    private void populateNavigationTargetFields(WebContentBannerUpsertRequest request, WebContentBanner banner) {
        WebContentBannerTargetType targetType = banner.getTargetType();
        if (targetType == null) {
            String legacyLink = normalize(banner.getNavigationLink());
            if (legacyLink == null) {
                request.setTargetType(WebContentBannerTargetType.NONE);
            } else {
                request.setTargetType(WebContentBannerTargetType.EXTERNAL_URL);
                request.setExternalUrl(legacyLink);
            }
            return;
        }

        request.setTargetType(targetType);
        switch (targetType) {
            case NONE -> {
            }
            case MOVIE -> request.setMovieTargetId(normalize(banner.getTargetRefId()));
            case SERIES -> request.setSeriesTargetId(normalize(banner.getTargetRefId()));
            case STATIC_PAGE -> {
                String targetRefId = normalize(banner.getTargetRefId());
                if (targetRefId != null) {
                    try {
                        request.setStaticPageTarget(WebStaticPageType.valueOf(targetRefId));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            case EXTERNAL_URL -> request.setExternalUrl(
                    normalize(banner.getExternalUrl()) != null ? normalize(banner.getExternalUrl()) : normalize(banner.getNavigationLink())
            );
        }
    }

    private String buildContentLabel(String title, ContentStatus status) {
        String safeTitle = normalize(title);
        String baseLabel = safeTitle != null ? safeTitle : "Không có tiêu đề";
        if (status == null || status == ContentStatus.ACTIVE) {
            return baseLabel;
        }
        return baseLabel + " [" + status.name() + "]";
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String sortKey(String value) {
        String normalized = normalize(value);
        return normalized != null ? normalized : "";
    }

    public record SelectOption(String value, String label) {
    }
}
