package com.hcmute.lovestream.dto.response;

public record WatchRoomStateResponse(
        String roomCode,
        String roomName,
        String status,
        boolean host,
        boolean privateRoom
) {
}

