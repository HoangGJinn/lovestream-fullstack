package com.hcmute.lovestream.controller.web.admin;

import com.hcmute.lovestream.entity.StaticPage;
import com.hcmute.lovestream.entity.enums.WebStaticPageType;
import com.hcmute.lovestream.service.webcontent.WebContentStaticPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/web-content/pages")
@RequiredArgsConstructor
@Slf4j
public class WebContentStaticPageAdminController {

    private final WebContentStaticPageService staticPageService;

    @GetMapping("/{pageType}/edit")
    public String edit(@PathVariable String pageType, Model model) {
        WebStaticPageType type = parseType(pageType);
        StaticPage page = staticPageService.getOrThrow(type);

        model.addAttribute("pageType", type);
        model.addAttribute("htmlContent", page.getHtmlContent());
        model.addAttribute("staticPageTypes", WebStaticPageType.values());
        return "admin/web-content/pages/edit";
    }

    @PostMapping("/{pageType}/edit")
    public String update(
            @PathVariable String pageType,
            @RequestParam("htmlContent") String htmlContent,
            Model model,
            RedirectAttributes redirectAttributes) {

        WebStaticPageType type = parseType(pageType);
        try {
            staticPageService.updateContent(type, htmlContent);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trang tĩnh thành công!");
            return "redirect:/admin/web-content?tab=static-pages";
        } catch (IllegalArgumentException e) {
            log.warn("Update static page failed: {}", e.getMessage());
            model.addAttribute("pageType", type);
            model.addAttribute("htmlContent", htmlContent);
            model.addAttribute("staticPageTypes", WebStaticPageType.values());
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/web-content/pages/edit";
        }
    }

    private WebStaticPageType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Trang tĩnh không hợp lệ.");
        }
        return WebStaticPageType.valueOf(raw.trim().toUpperCase());
    }
}

