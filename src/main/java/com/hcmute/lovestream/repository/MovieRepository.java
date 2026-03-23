package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, String> {
	Optional<Movie> findByIdAndStatus(String id, ContentStatus status);
}
