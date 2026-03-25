package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Episode;
import com.hcmute.lovestream.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpisodeRepository  extends JpaRepository<Episode, String> {
    List<Episode> findBySeasonOrderByEpisodeNumberAsc(Season season);
    boolean existsBySeasonAndEpisodeNumber(Season season, int episodeNumber);
}
