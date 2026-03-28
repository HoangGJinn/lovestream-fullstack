package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.service.notification.SeriesWatchStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/series-watch-state")
@RequiredArgsConstructor
public class SeriesWatchStateRestController {

    private final SeriesWatchStateService seriesWatchStateService;

    @PostMapping("/episodes/{episodeId}/touch")
    public ResponseEntity<Void> touchWatchingEpisode(@PathVariable String episodeId, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        seriesWatchStateService.touchWatchingEpisode(principal.getName(), episodeId);
        return ResponseEntity.noContent().build();
    }
}
