package com.hcmute.lovestream.controller;

import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.repository.VideoContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SitemapController {

    @Autowired
    private VideoContentRepository videoContentRepository;

    private final String BASE_URL = "https://lovestream-fullstack-production.up.railway.app";

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String createSitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1. Trang chủ (ưu tiên cao nhất 1.0)
        xml.append("  <url>\n");
        xml.append("    <loc>").append(BASE_URL).append("</loc>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        // 2. Trang tìm kiếm phim
        xml.append("  <url>\n");
        xml.append("    <loc>").append(BASE_URL).append("/videocontents/search</loc>\n");
        xml.append("    <priority>0.9</priority>\n");
        xml.append("  </url>\n");

        // 3. Trang gói dịch vụ
        xml.append("  <url>\n");
        xml.append("    <loc>").append(BASE_URL).append("/packages</loc>\n");
        xml.append("    <priority>0.8</priority>\n");
        xml.append("  </url>\n");

        // 4. Tự động lấy danh sách phim/series ACTIVE từ Database
        List<VideoContent> contents = videoContentRepository.findAll().stream()
                .filter(v -> v.getStatus() == ContentStatus.ACTIVE)
                .toList();

        for (VideoContent content : contents) {
            String urlPath = (content.getSlug() != null && !content.getSlug().isBlank())
                    ? content.getSlug()
                    : content.getId();
            xml.append("  <url>\n");
            xml.append("    <loc>").append(BASE_URL).append("/movies/").append(urlPath).append("</loc>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");
        return xml.toString();
    }
}
