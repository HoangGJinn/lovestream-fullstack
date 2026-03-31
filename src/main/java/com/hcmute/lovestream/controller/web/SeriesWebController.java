package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.service.videoContent.SeriesDetailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SeriesWebController {

    private final SeriesDetailService seriesDetailService;

    /**
     * GET /series/{seriesId}
     * Trang chi ti\u1ebft TV Series: danh s\u00e1ch m\u00f9a, t\u1eadp, v\u00e0 n\u00fat xem.
     */
    @GetMapping("/series/{seriesId}")
    public String seriesDetail(
            @PathVariable String seriesId,
            // c\u00e1c bi\u1ebfn n\u00e0y \u0111\u01b0\u1ee3c ti\u00eam t\u1ef1 \u0111\u1ed9ng b\u1edfi GlobalModelAttributes
            @org.springframework.web.bind.annotation.ModelAttribute("isAuthenticated") boolean isAuthenticated,
            @org.springframework.web.bind.annotation.ModelAttribute("hasActiveSub") boolean hasActiveSub,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {
        model.addAttribute("queryString", request.getQueryString());

        try {
            SeriesDetailService.SeriesDetailDto detail =
                    seriesDetailService.getSeriesDetail(seriesId, isAuthenticated, hasActiveSub);
            model.addAttribute("series", detail);
            return "videocontent/series/detail";
        } catch (RuntimeException e) {
            log.warn("Series detail error for id={}: {}", seriesId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/series";
        }
    }
}
