package com.hcmute.lovestream.dto.response;

import java.time.LocalDateTime;

public record DeviceAccessItemResponse(
        String deviceId,
        String deviceName,
        String os,
        LocalDateTime lastLogin,
        boolean currentDevice,
        boolean active,
        boolean streaming
) {
}
