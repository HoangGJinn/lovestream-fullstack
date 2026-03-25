package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Season;
import com.hcmute.lovestream.entity.TVSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonRepository extends JpaRepository<Season, String> {
    List<Season> findByTvSeriesOrderBySeasonNumberAsc(TVSeries tvSeries);
    boolean existsByTvSeriesAndSeasonNumber(TVSeries tvSeries, int seasonNumber);
}
