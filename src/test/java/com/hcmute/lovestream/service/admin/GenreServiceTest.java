package com.hcmute.lovestream.service.admin;

import com.hcmute.lovestream.dto.request.admin.GenreRequest;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.service.admin.impl.GenreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreServiceImpl genreService;

    private Genre testGenre;

    @BeforeEach
    void setUp() {
        testGenre = new Genre();
        testGenre.setId("genre-1");
        testGenre.setName("Hành động");
        testGenre.setVideoContents(new ArrayList<>());
    }

    @Test
    void getAllGenres_SortingByNameAsc() {
        Genre genre2 = new Genre();
        genre2.setName("Kinh dị");
        
        when(genreRepository.findAll()).thenReturn(List.of(genre2, testGenre));
        
        List<Genre> result = genreService.getAllGenres("name-asc");
        
        assertEquals(2, result.size());
        assertEquals("Hành động", result.get(0).getName());
        assertEquals("Kinh dị", result.get(1).getName());
    }

    @Test
    void getAllGenres_SortingByCountDesc() {
        Genre genre2 = new Genre();
        genre2.setName("Kinh dị");
        genre2.setVideoContents(List.of(mock(VideoContent.class), mock(VideoContent.class)));
        
        testGenre.setVideoContents(List.of(mock(VideoContent.class)));
        
        when(genreRepository.findAll()).thenReturn(List.of(testGenre, genre2));
        
        List<Genre> result = genreService.getAllGenres("count-desc");
        
        assertEquals(2, result.size());
        assertEquals("Kinh dị", result.get(0).getName());
        assertEquals("Hành động", result.get(1).getName());
    }

    @Test
    void createGenre_Success() {
        GenreRequest request = new GenreRequest("  Kinh dị  ");
        when(genreRepository.existsByNameIgnoreCase("Kinh dị")).thenReturn(false);
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Genre created = genreService.createGenre(request);

        assertNotNull(created);
        assertEquals("Kinh dị", created.getName());
        verify(genreRepository).save(any(Genre.class));
    }

    @Test
    void createGenre_DuplicateName_ThrowsException() {
        GenreRequest request = new GenreRequest("Hành động");
        when(genreRepository.existsByNameIgnoreCase("Hành động")).thenReturn(true);
        when(genreRepository.findByName("Hành động")).thenReturn(Optional.of(testGenre));

        Exception exception = assertThrows(RuntimeException.class, () -> genreService.createGenre(request));
        assertTrue(exception.getMessage().contains("đã tồn tại"));
    }

    @Test
    void createGenre_InvalidCharacters_ThrowsException() {
        GenreRequest request = new GenreRequest("Hành động @#$");

        Exception exception = assertThrows(RuntimeException.class, () -> genreService.createGenre(request));
        assertTrue(exception.getMessage().contains("ký tự không hợp lệ"));
    }

    @Test
    void updateGenre_Success() {
        GenreRequest request = new GenreRequest("Hành động mới");
        when(genreRepository.findById("genre-1")).thenReturn(Optional.of(testGenre));
        when(genreRepository.existsByNameIgnoreCase("Hành động mới")).thenReturn(false);
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Genre updated = genreService.updateGenre("genre-1", request);

        assertNotNull(updated);
        assertEquals("Hành động mới", updated.getName());
    }

    @Test
    void deleteGenre_SafeUnlinking() {
        VideoContent mockVideo = mock(VideoContent.class);
        Set<Genre> movieGenres = new HashSet<>();
        movieGenres.add(testGenre);
        when(mockVideo.getGenres()).thenReturn(movieGenres);
        
        testGenre.setVideoContents(new ArrayList<>(List.of(mockVideo)));
        when(genreRepository.findById("genre-1")).thenReturn(Optional.of(testGenre));

        genreService.deleteGenre("genre-1");

        assertTrue(movieGenres.isEmpty());
        verify(genreRepository).delete(testGenre);
    }
}
