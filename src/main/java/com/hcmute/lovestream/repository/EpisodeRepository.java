package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository  extends JpaRepository<Episode, String> {
}
