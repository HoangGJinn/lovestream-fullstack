package com.hcmute.lovestream.service.notification;

import com.hcmute.lovestream.entity.Episode;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.TypeNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeriesReleaseNotificationService {

    private final SeriesWatchStateService seriesWatchStateService;
    private final NotificationService notificationService;

    @Transactional
    public int notifyEpisodeAvailable(Episode episode) {
        if (episode == null || episode.getSeason() == null || episode.getSeason().getTvSeries() == null) {
            return 0;
        }

        TVSeries series = episode.getSeason().getTvSeries();
        List<User> watchers = seriesWatchStateService.findUsersWatchingSeries(series.getId());
        if (watchers.isEmpty()) {
            return 0;
        }

        int sentCount = 0;
        String targetUrl = "/watch-episode?episodeId=" + episode.getId();
        String title = "Series co tap moi";
        String content = buildContent(series, episode);

        for (User user : watchers) {
            String dedupeKey = "CONTENT_RELEASE:" + episode.getId() + ":" + user.getId();
            if (notificationService.createNotification(
                    user,
                    TypeNotification.CONTENT_RELEASE,
                    title,
                    content,
                    targetUrl,
                    dedupeKey
            ) != null) {
                sentCount++;
            }
        }

        if (sentCount > 0) {
            log.info("Created {} content release notifications for episode {}", sentCount, episode.getId());
        }
        return sentCount;
    }

    private String buildContent(TVSeries series, Episode episode) {
        StringBuilder builder = new StringBuilder();
        builder.append(series.getTitle())
                .append(" da co tap ")
                .append(episode.getEpisodeNumber());

        if (episode.getSeason() != null && episode.getSeason().getSeasonNumber() > 0) {
            builder.append(" mua ").append(episode.getSeason().getSeasonNumber());
        }

        if (episode.getTitle() != null && !episode.getTitle().isBlank()) {
            builder.append(": ").append(episode.getTitle().trim());
        }

        builder.append(". Mo ngay de xem.");
        return builder.toString();
    }
}
