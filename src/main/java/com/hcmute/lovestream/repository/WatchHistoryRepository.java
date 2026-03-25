package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.WatchHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, String> {

    @EntityGraph(attributePaths = {"videoContent", "videoContent.mediaAssets"})
    List<WatchHistory> findByUserIdOrderByLastWatchedAtDesc(String userId);

    Optional<WatchHistory> findByUserIdAndVideoContentId(String userId, String videoContentId);

    long deleteByUserIdAndVideoContentId(String userId, String videoContentId);
}

