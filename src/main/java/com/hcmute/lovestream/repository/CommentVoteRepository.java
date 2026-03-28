package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Comment;
import com.hcmute.lovestream.entity.CommentVote;
import com.hcmute.lovestream.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, String> {

    Optional<CommentVote> findByUserAndComment(User user, Comment comment);

}
