package com.hcmute.lovestream.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RatingRequest {
    @NotBlank(message = "ID phim không được để trống")
    private String videoContentId;
    @Min(value = 1, message = "Điểm phải từ 1 sao")
    @Max(value = 5, message = "Điểm tối đa là 5 sao")
    private int score;
}
