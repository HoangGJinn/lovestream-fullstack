package com.hcmute.lovestream.runner;

import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.repository.VideoContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backfill slug cho tất cả các VideoContent chưa có slug.
 * Chạy tự động 1 lần khi app khởi động.
 * An toàn khi chạy nhiều lần: chỉ xử lý record có slug == null.
 */
//@Component
@RequiredArgsConstructor
@Slf4j
public class SlugBackfillRunner implements ApplicationRunner {

    private final VideoContentRepository videoContentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
//        List<VideoContent> all = videoContentRepository.findAll();
//        int count = 0;
//
//        for (VideoContent vc : all) {
//            if (vc.getSlug() == null || vc.getSlug().isBlank()) {
//                String slug = VideoContent.generateSlug(vc.getTitle());
//                vc.setSlug(slug);
//                // Nếu title bị null/trống, fallback về id
//                if (slug.isBlank()) {
//                    vc.setSlug(vc.getId());
//                }
//                count++;
//            }
//        }
//
//        if (count > 0) {
//            videoContentRepository.saveAll(all);
//            log.info("[SlugBackfill] Đã backfill slug cho {} VideoContent records.", count);
//        } else {
//            log.info("[SlugBackfill] Tất cả record đã có slug, không cần backfill.");
//        }
    }
}
