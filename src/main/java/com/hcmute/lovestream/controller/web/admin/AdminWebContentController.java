package com.hcmute.lovestream.controller.web.admin;

import com.hcmute.lovestream.entity.StaticPage;
import com.hcmute.lovestream.entity.WebContentBanner;
import com.hcmute.lovestream.entity.enums.WebStaticPageType;
import com.hcmute.lovestream.repository.StaticPageRepository;
import com.hcmute.lovestream.service.webcontent.WebContentBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin/web-content")
@RequiredArgsConstructor
public class AdminWebContentController {

    private final WebContentBannerService bannerService;
    private final StaticPageRepository staticPageRepository;

    @GetMapping
    public String index(
            @RequestParam(defaultValue = "banners") String tab,
            Model model) {

        model.addAttribute("tab", tab);

        List<WebContentBanner> banners = bannerService.getAllOrdered();
        model.addAttribute("banners", banners);

        List<StaticPage> staticPages = staticPageRepository.findAll();
        model.addAttribute("staticPages", staticPages);
        model.addAttribute("staticPageTypes", WebStaticPageType.values());

        model.addAttribute("aboutPage", staticPageRepository.findByPageType(WebStaticPageType.ABOUT).orElse(null));
        model.addAttribute("privacyPolicyPage",
                staticPageRepository.findByPageType(WebStaticPageType.PRIVACY_POLICY).orElse(null));
        model.addAttribute("termsPage", staticPageRepository.findByPageType(WebStaticPageType.TERMS).orElse(null));

        return "admin/web-content/index";
    }
}

