package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.WebContentBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebContentBannerRepository extends JpaRepository<WebContentBanner, Long> {

    List<WebContentBanner> findAllByOrderByDisplayOrderAsc();

    List<WebContentBanner> findByIsDisplayedTrueOrderByDisplayOrderAsc();
}

