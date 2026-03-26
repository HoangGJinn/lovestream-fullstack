package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.StaticPage;
import com.hcmute.lovestream.entity.enums.WebStaticPageType;
import com.hcmute.lovestream.service.webcontent.WebContentStaticPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class WebContentPublicController {

    private final WebContentStaticPageService staticPageService;

    @GetMapping("/about")
    public String about(Model model) {
        return renderPage(WebStaticPageType.ABOUT, model);
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model) {
        return renderPage(WebStaticPageType.PRIVACY_POLICY, model);
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        return renderPage(WebStaticPageType.TERMS, model);
    }

    private String renderPage(WebStaticPageType type, Model model) {
        StaticPage page = staticPageService.getOrThrow(type);
        model.addAttribute("pageType", type);
        model.addAttribute("htmlContent", page.getHtmlContent());
        return "web-content/static-page";
    }
}

