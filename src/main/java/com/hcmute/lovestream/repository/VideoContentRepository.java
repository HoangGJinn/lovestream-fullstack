package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.VideoContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoContentRepository extends JpaRepository<VideoContent, String>, JpaSpecificationExecutor<VideoContent> {

    List<VideoContent> findByGenres_Name(String genreName);

    @Override
    @EntityGraph(attributePaths = {"genres", "mediaAssets"})
    Page<VideoContent> findAll(Specification<VideoContent> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"genres", "mediaAssets"})
    Optional<VideoContent> findById(String id);

}