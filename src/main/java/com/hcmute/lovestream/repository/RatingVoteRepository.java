package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Rating;
import com.hcmute.lovestream.entity.RatingVote;
import com.hcmute.lovestream.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingVoteRepository extends JpaRepository<RatingVote, String> {
    Optional<RatingVote> findTop1ByUserAndRating(User user, Rating rating);
}
