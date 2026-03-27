package com.hcmute.lovestream.dto.request.contentmanager.series;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonUpsertRequest {
    private String id;
    private String tvSeriesId;

    @NotNull(message = "Số mùa không được để trống")
    @Min(value = 1, message = "Số mùa phải lớn hơn 0")
    private Integer seasonNumber;

    /** Tên mùa — tùy chọn */
    private String name;

    @NotNull(message = "Năm phát hành không được để trống")
    @Min(value = 1800, message = "Năm phát hành không hợp lệ")
    private Integer releaseYear;
}
