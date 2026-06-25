package com.hcmute.lovestream.service.videoContent;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.repository.FavoriteListRepository;
import com.hcmute.lovestream.repository.RatingRepository;
import com.hcmute.lovestream.service.videoContent.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class MovieSortStrategyTest {

    private FavoriteListRepository favoriteListRepository;
    private RatingRepository ratingRepository;

    private PopularitySortStrategy popularitySortStrategy;
    private NewestSortStrategy newestSortStrategy;
    private TopRatedSortStrategy topRatedSortStrategy;
    private AlphabeticalSortStrategy alphabeticalSortStrategy;
    private ReverseAlphabeticalSortStrategy reverseAlphabeticalSortStrategy;
    private DurationAscSortStrategy durationAscSortStrategy;
    private DurationDescSortStrategy durationDescSortStrategy;
    private RecommendedSortStrategy recommendedSortStrategy;

    private Movie inception;
    private Movie interstellar;
    private Movie memento;

    @BeforeEach
    public void setUp() {
        favoriteListRepository = Mockito.mock(FavoriteListRepository.class);
        ratingRepository = Mockito.mock(RatingRepository.class);

        popularitySortStrategy = new PopularitySortStrategy();
        newestSortStrategy = new NewestSortStrategy();
        topRatedSortStrategy = new TopRatedSortStrategy();
        alphabeticalSortStrategy = new AlphabeticalSortStrategy();
        reverseAlphabeticalSortStrategy = new ReverseAlphabeticalSortStrategy();
        durationAscSortStrategy = new DurationAscSortStrategy();
        durationDescSortStrategy = new DurationDescSortStrategy();
        recommendedSortStrategy = new RecommendedSortStrategy(favoriteListRepository, ratingRepository);

        inception = new Movie();
        inception.setId("1");
        inception.setTitle("Inception");
        inception.setDurationMinutes(148);
        inception.setReleaseDate(new GregorianCalendar(2010, Calendar.JULY, 16).getTime());

        interstellar = new Movie();
        interstellar.setId("2");
        interstellar.setTitle("Interstellar");
        interstellar.setDurationMinutes(169);
        interstellar.setReleaseDate(new GregorianCalendar(2014, Calendar.NOVEMBER, 7).getTime());

        memento = new Movie();
        memento.setId("3");
        memento.setTitle("Memento");
        memento.setDurationMinutes(113);
        memento.setReleaseDate(new GregorianCalendar(2000, Calendar.SEPTEMBER, 5).getTime());
    }

    @Test
    public void testSupports() {
        assertTrue(popularitySortStrategy.supports("popularity"));
        assertFalse(popularitySortStrategy.supports("newest"));

        assertTrue(newestSortStrategy.supports("newest"));
        assertTrue(topRatedSortStrategy.supports("top_rated"));
        assertTrue(alphabeticalSortStrategy.supports("az"));
        assertTrue(reverseAlphabeticalSortStrategy.supports("za"));
        assertTrue(durationAscSortStrategy.supports("duration_asc"));
        assertTrue(durationDescSortStrategy.supports("duration_desc"));
        assertTrue(recommendedSortStrategy.supports("recommended"));
        assertTrue(recommendedSortStrategy.supports("default"));
    }

    @Test
    public void testAlphabeticalSort() {
        List<Movie> list = Arrays.asList(interstellar, inception, memento);
        list.sort(alphabeticalSortStrategy.getComparator(Map.of(), Map.of(), Map.of(), Optional.empty()));
        assertEquals("Inception", list.get(0).getTitle());
        assertEquals("Interstellar", list.get(1).getTitle());
        assertEquals("Memento", list.get(2).getTitle());
    }

    @Test
    public void testReverseAlphabeticalSort() {
        List<Movie> list = Arrays.asList(interstellar, inception, memento);
        list.sort(reverseAlphabeticalSortStrategy.getComparator(Map.of(), Map.of(), Map.of(), Optional.empty()));
        assertEquals("Memento", list.get(0).getTitle());
        assertEquals("Interstellar", list.get(1).getTitle());
        assertEquals("Inception", list.get(2).getTitle());
    }

    @Test
    public void testNewestSort() {
        List<Movie> list = Arrays.asList(memento, interstellar, inception);
        list.sort(newestSortStrategy.getComparator(Map.of(), Map.of(), Map.of(), Optional.empty()));
        assertEquals("Interstellar", list.get(0).getTitle()); // 2014
        assertEquals("Inception", list.get(1).getTitle());    // 2010
        assertEquals("Memento", list.get(2).getTitle());      // 2000
    }

    @Test
    public void testDurationSort() {
        List<Movie> list = Arrays.asList(interstellar, inception, memento);
        
        // Ascending
        list.sort(durationAscSortStrategy.getComparator(Map.of(), Map.of(), Map.of(), Optional.empty()));
        assertEquals("Memento", list.get(0).getTitle());      // 113
        assertEquals("Inception", list.get(1).getTitle());    // 148
        assertEquals("Interstellar", list.get(2).getTitle()); // 169

        // Descending
        list.sort(durationDescSortStrategy.getComparator(Map.of(), Map.of(), Map.of(), Optional.empty()));
        assertEquals("Interstellar", list.get(0).getTitle()); // 169
        assertEquals("Inception", list.get(1).getTitle());    // 148
        assertEquals("Memento", list.get(2).getTitle());      // 113
    }

    @Test
    public void testPopularitySort() {
        // Inception has 10 ratings, 5 favorites -> score = 5*2 + 10 = 20
        // Interstellar has 8 ratings, 8 favorites -> score = 8*2 + 8 = 24
        // Memento has 2 ratings, 1 favorite -> score = 1*2 + 2 = 4
        Map<String, Long> ratingCounts = Map.of("1", 10L, "2", 8L, "3", 2L);
        Map<String, Long> favoriteCounts = Map.of("1", 5L, "2", 8L, "3", 1L);

        List<Movie> list = Arrays.asList(inception, interstellar, memento);
        list.sort(popularitySortStrategy.getComparator(Map.of(), ratingCounts, favoriteCounts, Optional.empty()));

        assertEquals("Interstellar", list.get(0).getTitle()); // score 24
        assertEquals("Inception", list.get(1).getTitle());    // score 20
        assertEquals("Memento", list.get(2).getTitle());      // score 4
    }
}
