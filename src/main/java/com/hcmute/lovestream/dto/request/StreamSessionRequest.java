package com.hcmute.lovestream.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StreamSessionRequest {

    @NotBlank(message = "deviceId không được để trống")
    @Size(max = 128, message = "deviceId tối đa 128 ký tự")
    private String deviceId;

    @Size(max = 128, message = "videoContentId tối đa 128 ký tự")
    private String videoContentId;
}
