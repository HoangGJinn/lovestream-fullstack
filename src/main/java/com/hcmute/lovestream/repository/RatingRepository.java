package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingRepository extends JpaRepository<Rating, String> {
}
