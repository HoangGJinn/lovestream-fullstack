package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.VideoContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoContentRepository extends JpaRepository<VideoContent, String> {

    List<VideoContent> findByGenres_Name(String genreName);

}