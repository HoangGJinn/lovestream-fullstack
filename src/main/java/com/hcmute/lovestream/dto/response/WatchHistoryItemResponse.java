package com.hcmute.lovestream.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WatchHistoryItemResponse {
    private String videoContentId;
    private String title;
    private String description;
    private String posterUrl;
    private Double progressSeconds;
    private Double durationSeconds;
    private Integer progressPercent;
    private boolean completed;
    private LocalDateTime lastWatchedAt;
    private String watchUrl;
    private boolean available;

}

