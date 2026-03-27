package com.hcmute.lovestream.service.contentmanager;

import com.hcmute.lovestream.dto.request.contentmanager.GenreRequest;
import com.hcmute.lovestream.entity.Genre;

import java.util.List;

public interface GenreService {
    List<Genre> getAllGenres(String sort);
    Genre getGenreById(String id);
    Genre createGenre(GenreRequest request);
    Genre updateGenre(String id, GenreRequest request);
    void deleteGenre(String id);
    long countVideoContentByGenre(String id);
}
