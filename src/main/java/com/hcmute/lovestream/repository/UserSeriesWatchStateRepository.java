package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.UserSeriesWatchState;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSeriesWatchStateRepository extends JpaRepository<UserSeriesWatchState, String> {

    Optional<UserSeriesWatchState> findByUser_IdAndSeries_Id(String userId, String seriesId);

    @EntityGraph(attributePaths = {"user"})
    List<UserSeriesWatchState> findBySeries_IdAndNotificationsEnabledTrue(String seriesId);

    void deleteBySeries_Id(String seriesId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserSeriesWatchState watchState
               set watchState.lastWatchedEpisode = null
             where watchState.lastWatchedEpisode.id = :episodeId
            """)
    int clearLastWatchedEpisode(@Param("episodeId") String episodeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserSeriesWatchState watchState
               set watchState.lastWatchedEpisode = null
             where watchState.lastWatchedEpisode.season.id = :seasonId
            """)
    int clearLastWatchedEpisodeBySeasonId(@Param("seasonId") String seasonId);
}
