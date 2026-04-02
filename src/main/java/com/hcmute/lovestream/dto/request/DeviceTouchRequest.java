package com.hcmute.lovestream.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeviceTouchRequest {

    @NotBlank(message = "deviceId khong duoc de trong")
    @Size(max = 128, message = "deviceId toi da 128 ky tu")
    private String deviceId;
}
