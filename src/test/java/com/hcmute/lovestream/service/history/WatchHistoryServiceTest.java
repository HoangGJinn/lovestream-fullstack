package com.hcmute.lovestream.service.history;

import com.hcmute.lovestream.dto.response.WatchHistoryItemResponse;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.WatchHistory;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.repository.VideoContentRepository;
import com.hcmute.lovestream.repository.WatchHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchHistoryServiceTest {

    @Mock
    private WatchHistoryRepository watchHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoContentRepository videoContentRepository;

    @InjectMocks
    private WatchHistoryService watchHistoryService;

    @Test
    void getHistoryByUser_shouldPreserveAvailabilityPerVideoStatus() {
        User user = User.builder()
                .id("user-1")
                .email("demo@lovestream.local")
                .fullName("Demo User")
                .password("secret")
                .build();

        Movie activeMovie = new Movie();
        activeMovie.setId("movie-active");
        activeMovie.setTitle("Glee");
        activeMovie.setDescription("Active movie");
        activeMovie.setStatus(ContentStatus.ACTIVE);

        Movie hiddenMovie = new Movie();
        hiddenMovie.setId("movie-hidden");
        hiddenMovie.setTitle("The Flash");
        hiddenMovie.setDescription("Hidden movie");
        hiddenMovie.setStatus(ContentStatus.HIDDEN);

        WatchHistory activeHistory = new WatchHistory();
        activeHistory.setUser(user);
        activeHistory.setVideoContent(activeMovie);
        activeHistory.setProgressSeconds(120.0);
        activeHistory.setDurationSeconds(600.0);
        activeHistory.setLastWatchedAt(LocalDateTime.now());

        WatchHistory hiddenHistory = new WatchHistory();
        hiddenHistory.setUser(user);
        hiddenHistory.setVideoContent(hiddenMovie);
        hiddenHistory.setProgressSeconds(60.0);
        hiddenHistory.setDurationSeconds(600.0);
        hiddenHistory.setLastWatchedAt(LocalDateTime.now().minusMinutes(5));

        when(userRepository.findByEmail("demo@lovestream.local")).thenReturn(Optional.of(user));
        when(watchHistoryRepository.findByUserIdOrderByLastWatchedAtDesc("user-1"))
                .thenReturn(List.of(activeHistory, hiddenHistory));

        List<WatchHistoryItemResponse> items = watchHistoryService.getHistoryByUser("demo@lovestream.local");

        assertEquals(2, items.size());

        WatchHistoryItemResponse activeItem = items.get(0);
        assertTrue(activeItem.isAvailable());
        assertEquals("/watch-movie?id=movie-active", activeItem.getWatchUrl());

        WatchHistoryItemResponse hiddenItem = items.get(1);
        assertFalse(hiddenItem.isAvailable());
        assertNull(hiddenItem.getWatchUrl());
    }
}
