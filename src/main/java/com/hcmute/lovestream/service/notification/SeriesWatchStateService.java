package com.hcmute.lovestream.service.notification;

import com.hcmute.lovestream.entity.Episode;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.UserSeriesWatchState;
import com.hcmute.lovestream.repository.EpisodeRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.repository.UserSeriesWatchStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeriesWatchStateService {

    private final UserRepository userRepository;
    private final EpisodeRepository episodeRepository;
    private final UserSeriesWatchStateRepository userSeriesWatchStateRepository;

    @Transactional
    public void touchWatchingEpisode(String userEmail, String episodeId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"));
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tap phim"));

        if (episode.getSeason() == null || episode.getSeason().getTvSeries() == null) {
            throw new RuntimeException("Tap phim nay khong thuoc series hop le");
        }

        TVSeries series = episode.getSeason().getTvSeries();
        UserSeriesWatchState watchState = userSeriesWatchStateRepository
                .findByUser_IdAndSeries_Id(user.getId(), series.getId())
                .orElseGet(UserSeriesWatchState::new);

        watchState.setUser(user);
        watchState.setSeries(series);
        watchState.setLastWatchedEpisode(episode);
        watchState.setLastWatchedAt(LocalDateTime.now());
        watchState.setNotificationsEnabled(true);

        userSeriesWatchStateRepository.save(watchState);
    }

    @Transactional(readOnly = true)
    public List<User> findUsersWatchingSeries(String seriesId) {
        return userSeriesWatchStateRepository.findBySeries_IdAndNotificationsEnabledTrue(seriesId)
                .stream()
                .map(UserSeriesWatchState::getUser)
                .toList();
    }
}
