package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.ContentStatus;
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

    // Đã bỏ EntityGraph để tối ưu RAM (tránh Cartesian product), Hibernate sẽ tự dùng BatchSize IN fetch media_assets.
    List<VideoContent> findByGenres_Name(String genreName);

    // Đã bỏ EntityGraph
    List<VideoContent> findDistinctByStatusAndGenres_Name(ContentStatus status, String genreName);

    @EntityGraph(attributePaths = {"mediaAssets"})
    Page<VideoContent> findDistinctByStatusAndGenres_NameOrderByReleaseYearDesc(
            ContentStatus status,
            String genreName,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = {"mediaAssets"})
    Page<VideoContent> findAll(Specification<VideoContent> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"mediaAssets"})
    Optional<VideoContent> findById(String id);

    Optional<VideoContent> findByIdAndStatus(String id, ContentStatus status);

    Optional<VideoContent> findBySlugAndStatus(String slug, ContentStatus status);
}