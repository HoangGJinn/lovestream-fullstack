package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.TVSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TVSeriesRepository extends JpaRepository<TVSeries, String> {

}
