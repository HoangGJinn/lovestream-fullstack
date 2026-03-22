package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, String> {
    // Thêm hàm này để tìm Thể loại theo tên
    Optional<Genre> findByName(String name);
}