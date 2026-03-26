package com.hcmute.lovestream.controller.websocket;

import com.hcmute.lovestream.dto.websocket.RoomSyncMessage;
import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.service.watchtogether.RoomSessionTracker;
import com.hcmute.lovestream.service.watchtogether.WatchTogetherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RoomSyncController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final WatchTogetherService watchTogetherService;
    private final RoomSessionTracker roomSessionTracker;

    @MessageMapping("/room.sync")
    public void syncVideo(
            @Payload RoomSyncMessage message,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        if (message == null || principal == null || principal.getName() == null) {
            return;
        }

        String roomCode = normalizeRoomCode(message.getRoomCode());
        String action = normalizeAction(message.getAction());
        if (roomCode == null || action == null) {
            return;
        }

        String senderEmail = principal.getName();
        String sessionId = headerAccessor.getSessionId();

        try {
            switch (action) {
                case "JOIN" -> handleJoin(roomCode, senderEmail, sessionId);
                case "SYNC_TIME" -> handleSyncTime(roomCode, senderEmail, message.getCurrentTime());
                case "PLAY", "PAUSE", "SEEK", "STOP" -> handleHostPlayback(roomCode, senderEmail, action, message.getCurrentTime());
                default -> log.debug("Unsupported room sync action: {}", action);
            }
        } catch (RuntimeException ex) {
            log.warn("Room sync action failed room={}, action={}, sender={}: {}", roomCode, action, senderEmail, ex.getMessage());
        }
    }

    private void handleJoin(String roomCode, String senderEmail, String sessionId) {
        watchTogetherService.markParticipantConnected(roomCode, senderEmail);
        boolean firstSessionForUser = roomSessionTracker.registerSession(sessionId, roomCode, senderEmail);
        if (!firstSessionForUser) {
            return;
        }

        long participantCount = watchTogetherService.countActiveParticipants(roomCode);
        String status = watchTogetherService.getRoomStatus(roomCode);
        double currentTime = watchTogetherService.getCurrentVideoTime(roomCode);

        RoomSyncMessage joinMsg = RoomSyncMessage.builder()
                .roomCode(roomCode)
                .action("JOIN")
                .sender(senderEmail)
                .currentParticipants(participantCount)
                .status(status)
                .currentTime(currentTime)
                .build();

        messagingTemplate.convertAndSend(topic(roomCode), joinMsg);
    }

    private void handleHostPlayback(String roomCode, String senderEmail, String action, Double currentTime) {
        if (!watchTogetherService.isUserHost(roomCode, senderEmail)) {
            log.warn("Security warning: non-host user {} attempted {} in room {}", senderEmail, action, roomCode);
            return;
        }

        Room room = watchTogetherService.applyHostPlaybackAction(roomCode, senderEmail, action, currentTime);
        long participantCount = watchTogetherService.countActiveParticipants(roomCode);

        RoomSyncMessage syncMsg = RoomSyncMessage.builder()
                .roomCode(roomCode)
                .action(action)
                .sender(senderEmail)
                .currentParticipants(participantCount)
                .status(room.getStatus().name())
                .currentTime(room.getCurrentVideoTime())
                .build();

        messagingTemplate.convertAndSend(topic(roomCode), syncMsg);
    }

    private void handleSyncTime(String roomCode, String senderEmail, Double currentTime) {
        if (!watchTogetherService.isUserHost(roomCode, senderEmail)) {
            log.warn("Security warning: non-host user {} attempted SYNC_TIME in room {}", senderEmail, roomCode);
            return;
        }
        watchTogetherService.updateCurrentVideoTime(roomCode, senderEmail, currentTime);
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            return null;
        }
        return roomCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        return action.trim().toUpperCase(Locale.ROOT);
    }

    private String topic(String roomCode) {
        return "/topic/room/" + roomCode;
    }
}
