package com.hcmute.lovestream.controller.web.admin;

import com.hcmute.lovestream.dto.request.webcontent.WebContentBannerReorderRequest;
import com.hcmute.lovestream.dto.request.webcontent.WebContentBannerUpsertRequest;
import com.hcmute.lovestream.entity.WebContentBanner;
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

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/web-content/banners")
@RequiredArgsConstructor
@Slf4j
public class WebContentBannerAdminController {

    private final WebContentBannerService bannerService;

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        int nextOrder = bannerService.getAllOrdered().size() + 1;
        WebContentBannerUpsertRequest request = WebContentBannerUpsertRequest.builder()
                .displayOrder(nextOrder)
                .isDisplayed(true)
                .build();
        model.addAttribute("bannerRequest", request);
        return "admin/web-content/banners/form";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            WebContentBanner banner = bannerService.getOrThrow(id);
            WebContentBannerUpsertRequest request = WebContentBannerUpsertRequest.builder()
                    .id(banner.getId())
                    .title(banner.getTitle())
                    .navigationLink(banner.getNavigationLink())
                    .displayOrder(banner.getDisplayOrder())
                    .isDisplayed(banner.getIsDisplayed())
                    .existingImagePath(banner.getImagePath())
                    .build();
            model.addAttribute("bannerRequest", request);
            return "admin/web-content/banners/form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/web-content?tab=banners";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("bannerRequest") WebContentBannerUpsertRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "admin/web-content/banners/form";
        }

        try {
            bannerService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo banner thành công!");
            return "redirect:/admin/web-content?tab=banners";
        } catch (IllegalArgumentException e) {
            log.warn("Create banner failed: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/web-content/banners/form";
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
            // Ensure existingImagePath is available for preview.
            WebContentBanner existing = bannerService.getOrThrow(id);
            request.setExistingImagePath(existing.getImagePath());
            return "admin/web-content/banners/form";
        }

        try {
            bannerService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật banner thành công!");
            return "redirect:/admin/web-content?tab=banners";
        } catch (IllegalArgumentException e) {
            log.warn("Update banner failed: {}", e.getMessage());
            WebContentBanner existing = bannerService.getOrThrow(id);
            request.setExistingImagePath(existing.getImagePath());
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/web-content/banners/form";
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
        return "redirect:/admin/web-content?tab=banners";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bannerService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa banner thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/web-content?tab=banners";
    }

    @PostMapping(value = "/reorder", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reorder(@RequestBody WebContentBannerReorderRequest request) {
        bannerService.reorder(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}

