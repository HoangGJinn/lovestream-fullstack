package com.hcmute.lovestream.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSyncMessage {
    private String roomCode;
    private String action;
    private Double currentTime;
    private Long currentParticipants;
    private String status;
    private String sender;
}
