package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @BeforeEach
    void setUp() {
        // Xóa data cũ nết tồn tại trong H2 Database scope
        movieRepository.deleteAll();

        // Seed Fake Data
        Movie movieA = new Movie();
        movieA.setTitle("Avatar 2");
        movieA.setStatus(ContentStatus.ACTIVE);
        
        Movie movieB = new Movie();
        movieB.setTitle("Batman");
        movieB.setStatus(ContentStatus.HIDDEN);

        Movie movieC = new Movie();
        movieC.setTitle("Avengers");
        movieC.setStatus(ContentStatus.ACTIVE);

        movieRepository.saveAll(List.of(movieA, movieB, movieC));
    }

    @Test
    void testFindAllByOrderByTitleAsc() {
        List<Movie> movies = movieRepository.findAllByOrderByTitleAsc();

        assertThat(movies).hasSize(3);
        // Kiểm tra đúng thứ tự ASC: Avatar 2, Avengers, Batman
        assertThat(movies.get(0).getTitle()).isEqualTo("Avatar 2");
        assertThat(movies.get(1).getTitle()).isEqualTo("Avengers");
        assertThat(movies.get(2).getTitle()).isEqualTo("Batman");
    }

    @Test
    void testFindAllByStatusOrderByTitleAsc() {
        List<Movie> activeMovies = movieRepository.findAllByStatusOrderByTitleAsc(ContentStatus.ACTIVE);
        assertThat(activeMovies).hasSize(2);
        assertThat(activeMovies).extracting(Movie::getTitle).containsExactly("Avatar 2", "Avengers");

        List<Movie> hiddenMovies = movieRepository.findAllByStatusOrderByTitleAsc(ContentStatus.HIDDEN);
        assertThat(hiddenMovies).hasSize(1);
        assertThat(hiddenMovies.get(0).getTitle()).isEqualTo("Batman");
    }

    @Test
    void testFindByTitleContainingIgnoreCaseOrderByTitleAsc() {
        // "av" matches "Avatar 2" and "Avengers"
        List<Movie> matched = movieRepository.findByTitleContainingIgnoreCaseOrderByTitleAsc("av");
        assertThat(matched).hasSize(2);
        assertThat(matched).extracting(Movie::getTitle).containsExactly("Avatar 2", "Avengers");

        // "BAT" matches "Batman"
        List<Movie> matchedBat = movieRepository.findByTitleContainingIgnoreCaseOrderByTitleAsc("BAT");
        assertThat(matchedBat).hasSize(1);
        assertThat(matchedBat.get(0).getTitle()).isEqualTo("Batman");
    }

    @Test
    void testFindByStatusAndTitleContainingIgnoreCaseOrderByTitleAsc() {
        // "A" matches Avatar 2, Avengers, Batman. But with HIDDEN status, only Batman matches.
        List<Movie> matchedHidden = movieRepository.findByStatusAndTitleContainingIgnoreCaseOrderByTitleAsc(ContentStatus.HIDDEN, "a");
        assertThat(matchedHidden).hasSize(1);
        assertThat(matchedHidden.get(0).getTitle()).isEqualTo("Batman");

        // "Av" matches Avatar 2, Avengers (Both are ACTIVE)
        List<Movie> matchedActive = movieRepository.findByStatusAndTitleContainingIgnoreCaseOrderByTitleAsc(ContentStatus.ACTIVE, "av");
        assertThat(matchedActive).hasSize(2);
    }

    @Test
    void testCountByStatus() {
        long activeCount = movieRepository.countByStatus(ContentStatus.ACTIVE);
        long hiddenCount = movieRepository.countByStatus(ContentStatus.HIDDEN);

        assertThat(activeCount).isEqualTo(2);
        assertThat(hiddenCount).isEqualTo(1);
    }
}
