package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, String> {

    Optional<Rating> findByUserIdAndVideoContentId(String userId, String videoContentId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.videoContent.id = :videoId")
    Double calculateAverageScoreByVideoId(String videoId);

    int countByVideoContentId(String videoContentId);
}
