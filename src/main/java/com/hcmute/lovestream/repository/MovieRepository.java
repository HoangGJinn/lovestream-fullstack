package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface MovieRepository extends JpaRepository<Movie, String> {
    Optional<Movie> findByIdAndStatus(String id, ContentStatus status);

    @EntityGraph(attributePaths = { "mediaAssets" })
    Optional<Movie> findDetailedByIdAndStatus(String id, ContentStatus status);

    // Slug-based lookups for SEO-friendly URLs
    // Chỉ load mediaAssets eager (giống UUID method) — tránh Cartesian product.
    // genres và contentCredits sẽ được Hibernate batch-fetch qua @BatchSize trên entity.
    Optional<Movie> findBySlug(String slug);

    @EntityGraph(attributePaths = { "mediaAssets" })
    Optional<Movie> findDetailedBySlugAndStatus(String slug, ContentStatus status);

    @Query("SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres")
    List<Movie> findAllWithGenres();

    // 1. Danh sách tất cả phim lẻ (Sắp xếp A-Z)
    List<Movie> findAllByOrderByTitleAsc();

    @EntityGraph(attributePaths = { "mediaAssets" })
    Page<Movie> findAllByOrderByTitleAsc(Pageable pageable);

    // 2. Lọc phim lẻ theo trạng thái (ASC)
    List<Movie> findAllByStatusOrderByTitleAsc(ContentStatus status);

    @EntityGraph(attributePaths = { "mediaAssets" })
    Page<Movie> findAllByStatusOrderByTitleAsc(ContentStatus status, Pageable pageable);

    // 3. Tìm phim theo tên (Chứa từ khóa, không phân biệt hoa thường, ASC)
    List<Movie> findByTitleContainingIgnoreCaseOrderByTitleAsc(String keyword);

    @EntityGraph(attributePaths = { "mediaAssets" })
    Page<Movie> findByTitleContainingIgnoreCaseOrderByTitleAsc(String keyword, Pageable pageable);

    // 4. Kết hợp tìm tên và lọc trạng thái (ASC)
    List<Movie> findByStatusAndTitleContainingIgnoreCaseOrderByTitleAsc(ContentStatus status, String keyword);

    @EntityGraph(attributePaths = { "mediaAssets" })
    Page<Movie> findByStatusAndTitleContainingIgnoreCaseOrderByTitleAsc(ContentStatus status, String keyword,
            Pageable pageable);

    // 5. Đếm số lượng phim theo trạng thái
    long countByStatus(ContentStatus status);

    Page<Movie> findByTitleContainingIgnoreCaseAndStatus(String title, ContentStatus status, Pageable pageable);

    Page<Movie> findByStatus(ContentStatus status, Pageable pageable);

    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
