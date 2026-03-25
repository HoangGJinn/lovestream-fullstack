package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TVSeriesRepository extends JpaRepository<TVSeries, String> {
    List<TVSeries> findAllByStatus(ContentStatus status);

    Page<TVSeries> findByTitleContainingIgnoreCaseAndStatus(String title, ContentStatus status, Pageable pageable);

    Page<TVSeries> findByStatus(ContentStatus status, Pageable pageable);

    Page<TVSeries> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
