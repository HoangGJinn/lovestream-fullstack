package com.hcmute.lovestream.controller.websocket;

import com.hcmute.lovestream.dto.websocket.RoomSyncMessage;
import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.service.watchtogether.RoomSessionTracker;
import com.hcmute.lovestream.service.watchtogether.WatchTogetherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomWebSocketEventListener {

    private final RoomSessionTracker roomSessionTracker;
    private final WatchTogetherService watchTogetherService;
    private final SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        Optional<RoomSessionTracker.DisconnectInfo> disconnectInfo = roomSessionTracker.unregisterSession(sessionId);
        if (disconnectInfo.isEmpty()) {
            return;
        }

        RoomSessionTracker.DisconnectInfo info = disconnectInfo.get();
        if (!info.lastSessionForUserInRoom()) {
            return;
        }

        try {
            boolean disconnectedUserIsHost = watchTogetherService.isUserHost(info.roomCode(), info.userEmail());
            watchTogetherService.markParticipantDisconnected(info.roomCode(), info.userEmail());

            Room roomAfterUpdate;
            if (disconnectedUserIsHost) {
                roomAfterUpdate = watchTogetherService.forceStopRoom(info.roomCode());
            } else {
                roomAfterUpdate = watchTogetherService.findRoomEntityByCode(info.roomCode())
                        .orElseThrow(() -> new IllegalArgumentException("Phong khong ton tai"));
            }

            long participantCount = watchTogetherService.countActiveParticipants(info.roomCode());
            RoomSyncMessage leaveMsg = RoomSyncMessage.builder()
                    .roomCode(info.roomCode())
                    .action("LEAVE")
                    .sender(info.userEmail())
                    .currentParticipants(participantCount)
                    .status(roomAfterUpdate.getStatus().name())
                    .currentTime(roomAfterUpdate.getCurrentVideoTime())
                    .build();

            messagingTemplate.convertAndSend("/topic/room/" + info.roomCode(), leaveMsg);
        } catch (RuntimeException ex) {
            log.debug("Ignore disconnect handling error for session {}: {}", sessionId, ex.getMessage());
        }
    }
}
