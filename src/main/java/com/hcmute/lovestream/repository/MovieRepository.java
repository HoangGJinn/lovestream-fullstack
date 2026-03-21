package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, String> {
}
