package com.hcmute.lovestream.service.webcontent.impl;

import com.hcmute.lovestream.entity.StaticPage;
import com.hcmute.lovestream.entity.enums.WebStaticPageType;
import com.hcmute.lovestream.repository.StaticPageRepository;
import com.hcmute.lovestream.service.webcontent.WebContentStaticPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class WebContentStaticPageServiceImpl implements WebContentStaticPageService {

    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");

    private final StaticPageRepository staticPageRepository;

    @Override
    public StaticPage getOrThrow(WebStaticPageType type) {
        if (type == null) {
            throw new IllegalArgumentException("Loại trang tĩnh không hợp lệ.");
        }
        return staticPageRepository.findByPageType(type)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy trang tĩnh: " + type));
    }

    @Override
    @Transactional
    public StaticPage updateContent(WebStaticPageType type, String htmlContent) {
        if (type == null) {
            throw new IllegalArgumentException("Loại trang tĩnh không hợp lệ.");
        }
        if (htmlContent == null) {
            throw new IllegalArgumentException("Nội dung không được để trống.");
        }
        if (isBlankHtml(htmlContent)) {
            throw new IllegalArgumentException("Nội dung không được để trống.");
        }

        StaticPage page = getOrThrow(type);
        page.setHtmlContent(htmlContent);
        return staticPageRepository.save(page);
    }

    private boolean isBlankHtml(String html) {
        String cleaned = TAG_PATTERN.matcher(html).replaceAll("");
        cleaned = cleaned.replace("&nbsp;", " ").trim();
        return cleaned.isBlank();
    }
}

