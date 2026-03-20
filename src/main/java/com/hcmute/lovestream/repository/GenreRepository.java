package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface GenreRepository extends JpaRepository<Genre, String> {

}
