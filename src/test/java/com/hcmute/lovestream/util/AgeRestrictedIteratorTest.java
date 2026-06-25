package com.hcmute.lovestream.util;

import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.AgeRating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class AgeRestrictedIteratorTest {

    private Movie kidsMovie;      // G
    private Movie teenMovie;      // PG_13
    private Movie matureMovie;    // R_16
    private Movie adultMovie;     // R_18
    private Movie unratedMovie;   // null rating

    private TVSeries pgSeries;    // PG_13
    private TVSeries adultSeries; // R_18

    @BeforeEach
    public void setUp() {
        kidsMovie = new Movie();
        kidsMovie.setTitle("Toy Story");
        kidsMovie.setAgeRating(AgeRating.G);

        teenMovie = new Movie();
        teenMovie.setTitle("Spider-Man");
        teenMovie.setAgeRating(AgeRating.PG_13);

        matureMovie = new Movie();
        matureMovie.setTitle("Stranger Things Movie");
        matureMovie.setAgeRating(AgeRating.R_16);

        adultMovie = new Movie();
        adultMovie.setTitle("Deadpool");
        adultMovie.setAgeRating(AgeRating.R_18);

        unratedMovie = new Movie();
        unratedMovie.setTitle("Home Video");
        unratedMovie.setAgeRating(null);

        pgSeries = new TVSeries();
        pgSeries.setTitle("Harry Potter Series");
        pgSeries.setAgeRating(AgeRating.PG_13);

        adultSeries = new TVSeries();
        adultSeries.setTitle("Game of Thrones");
        adultSeries.setAgeRating(AgeRating.R_18);
    }

    @Test
    public void testChildUser_Age10() {
        List<Movie> movies = Arrays.asList(kidsMovie, teenMovie, matureMovie, adultMovie, unratedMovie);
        AgeRestrictedIterator<Movie> iterator = new AgeRestrictedIterator<>(movies.iterator(), 10);

        assertTrue(iterator.hasNext());
        assertEquals("Toy Story", iterator.next().getTitle());
        assertTrue(iterator.hasNext());
        assertEquals("Home Video", iterator.next().getTitle());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    public void testTeenUser_Age15() {
        List<Movie> movies = Arrays.asList(kidsMovie, teenMovie, matureMovie, adultMovie, unratedMovie);
        AgeRestrictedIterator<Movie> iterator = new AgeRestrictedIterator<>(movies.iterator(), 15);

        assertTrue(iterator.hasNext());
        assertEquals("Toy Story", iterator.next().getTitle());
        assertTrue(iterator.hasNext());
        assertEquals("Spider-Man", iterator.next().getTitle());
        assertTrue(iterator.hasNext());
        assertEquals("Home Video", iterator.next().getTitle());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testAdultUser_Age20() {
        List<Movie> movies = Arrays.asList(kidsMovie, teenMovie, matureMovie, adultMovie, unratedMovie);
        AgeRestrictedIterator<Movie> iterator = new AgeRestrictedIterator<>(movies.iterator(), 20);

        assertTrue(iterator.hasNext());
        assertEquals("Toy Story", iterator.next().getTitle());
        assertTrue(iterator.hasNext());
        assertEquals("Spider-Man", iterator.next().getTitle());
        assertTrue(iterator.hasNext());
        assertEquals("Stranger Things Movie", iterator.next().getTitle());
        assertTrue(iterator.hasNext());
        assertEquals("Deadpool", iterator.next().getTitle());
        assertTrue(iterator.hasNext());
        assertEquals("Home Video", iterator.next().getTitle());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGenericVideoContentFilter_Age14() {
        List<VideoContent> contents = Arrays.asList(kidsMovie, teenMovie, matureMovie, adultMovie, pgSeries, adultSeries);
        AgeFilterIterable<VideoContent> iterable = new AgeFilterIterable<>(contents, 14);

        int count = 0;
        for (VideoContent content : iterable) {
            count++;
            // Should contain: kidsMovie (G), teenMovie (PG_13), pgSeries (PG_13)
            assertTrue(content.getAgeRating() == AgeRating.G || content.getAgeRating() == AgeRating.PG_13);
        }
        assertEquals(3, count);
    }
}
