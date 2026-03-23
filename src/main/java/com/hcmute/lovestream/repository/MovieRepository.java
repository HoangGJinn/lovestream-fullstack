package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, String> {

    // 1. Danh sách tất cả phim lẻ (Sắp xếp A-Z)
    List<Movie> findAllByOrderByTitleAsc();

    // 2. Lọc phim lẻ theo trạng thái (ASC)
    List<Movie> findAllByStatusOrderByTitleAsc(ContentStatus status);

    // 3. Tìm phim theo tên (Chứa từ khóa, không phân biệt hoa thường, ASC)
    List<Movie> findByTitleContainingIgnoreCaseOrderByTitleAsc(String keyword);

    // 4. Kết hợp tìm tên và lọc trạng thái (ASC)
    List<Movie> findByStatusAndTitleContainingIgnoreCaseOrderByTitleAsc(ContentStatus status, String keyword);

    // 5. Đếm số lượng phim theo trạng thái
    long countByStatus(ContentStatus status);
}
