package com.hcmute.lovestream.entity;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class CompositePatternTest {

    @Test
    public void testMovieCompositeLeaf() {
        Movie movie = new Movie();
        movie.setTitle("Inception");
        movie.setDurationMinutes(148);

        assertEquals("Inception", movie.getTitle());
        assertEquals(148, movie.getTotalDurationMinutes());
        
        // Test getDetails outputs without crash
        assertDoesNotThrow(movie::getDetails);
    }

    @Test
    public void testTVSeriesCompositeTree() {
        // Create Episodes
        Episode ep1 = new Episode();
        ep1.setEpisodeNumber(1);
        ep1.setTitle("Winter is Coming");
        ep1.setDurationInMinutes(60);

        Episode ep2 = new Episode();
        ep2.setEpisodeNumber(2);
        ep2.setTitle("The Kingsroad");
        ep2.setDurationInMinutes(55);

        Episode ep3 = new Episode();
        ep3.setEpisodeNumber(3);
        ep3.setTitle("Lord Snow");
        ep3.setDurationInMinutes(58);

        // Create Season
        Season season1 = new Season();
        season1.setName("Season 1");
        season1.setEpisodes(Arrays.asList(ep1, ep2));

        Season season2 = new Season();
        season2.setName("Season 2");
        season2.setEpisodes(Arrays.asList(ep3));

        // Create TVSeries
        TVSeries tvSeries = new TVSeries();
        tvSeries.setTitle("Game of Thrones");
        tvSeries.setSeasons(Arrays.asList(season1, season2));

        // Assert title and structures
        assertEquals("Game of Thrones", tvSeries.getTitle());
        assertEquals("Season 1", season1.getTitle());
        assertEquals("Winter is Coming", ep1.getTitle());

        // Assert durations
        assertEquals(60, ep1.getTotalDurationMinutes());
        assertEquals(55, ep2.getTotalDurationMinutes());
        assertEquals(115, season1.getTotalDurationMinutes());
        assertEquals(58, season2.getTotalDurationMinutes());
        assertEquals(173, tvSeries.getTotalDurationMinutes());

        // Test getDetails outputs without crash
        assertDoesNotThrow(tvSeries::getDetails);
    }
}
