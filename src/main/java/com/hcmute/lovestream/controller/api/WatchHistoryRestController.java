package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.request.WatchHistoryProgressRequest;
import com.hcmute.lovestream.dto.response.WatchHistoryItemResponse;
import com.hcmute.lovestream.service.history.WatchHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class WatchHistoryRestController {

    private final WatchHistoryService watchHistoryService;

    @PostMapping("/progress")
    public ResponseEntity<Void> recordProgress(Principal principal,
                                               @Valid @RequestBody WatchHistoryProgressRequest request) {
        watchHistoryService.recordProgress(principal.getName(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<WatchHistoryItemResponse>> getHistory(Principal principal) {
        return ResponseEntity.ok(watchHistoryService.getHistoryByUser(principal.getName()));
    }

    @DeleteMapping("/{videoContentId}")
    public ResponseEntity<Void> removeHistoryItem(Principal principal,
                                                  @PathVariable String videoContentId) {
        watchHistoryService.removeHistoryItem(principal.getName(), videoContentId);
        return ResponseEntity.noContent().build();
    }
}

