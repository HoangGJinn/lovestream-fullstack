package com.hcmute.lovestream.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class WatchHistoryProgressRequest {
    @NotBlank(message = "ID phim không được để trống")
    private String videoContentId;

    @PositiveOrZero(message = "Thời gian xem phải >= 0")
    private Double currentTimeSeconds;

    @PositiveOrZero(message = "Thời lượng phải >= 0")
    private Double durationSeconds;
}

