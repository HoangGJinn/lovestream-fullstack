package com.hcmute.lovestream.dto.request.contentmanager.series;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeUpsertRequest {
    private String id;
    private String seasonId;

    @NotNull(message = "Số tập không được để trống")
    @Min(value = 1, message = "Số tập phải lớn hơn 0")
    private Integer episodeNumber;

    @NotBlank(message = "Tiêu đề tập không được để trống")
    private String title;

    @NotNull(message = "Thời lượng không được để trống")
    @Min(value = 1, message = "Thời lượng phải lớn hơn 0")
    private Integer durationInMinutes;

    /** Ngày phát sóng — tùy chọn */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate airDate;
}
