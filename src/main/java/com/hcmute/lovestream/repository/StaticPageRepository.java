package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.StaticPage;
import com.hcmute.lovestream.entity.enums.WebStaticPageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaticPageRepository extends JpaRepository<StaticPage, Long> {
    Optional<StaticPage> findByPageType(WebStaticPageType pageType);

    boolean existsByPageType(WebStaticPageType pageType);
}

