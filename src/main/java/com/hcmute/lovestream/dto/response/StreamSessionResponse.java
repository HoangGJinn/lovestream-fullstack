package com.hcmute.lovestream.dto.response;

public record StreamSessionResponse(
        boolean allowed,
        int activeDevices,
        int maxDevices,
        String message
) {
}
