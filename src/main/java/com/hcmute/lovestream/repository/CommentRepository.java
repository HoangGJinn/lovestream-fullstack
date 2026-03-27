package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

    @EntityGraph(attributePaths = {"user", "replies", "replies.user"})
    List<Comment> findByVideo_IdAndParentCommentIsNullOrderByCreatedAtDesc(String videoContentId);
}
