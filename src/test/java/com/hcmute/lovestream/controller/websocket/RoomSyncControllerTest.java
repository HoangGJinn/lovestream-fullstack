package com.hcmute.lovestream.controller.websocket;

import com.hcmute.lovestream.dto.websocket.RoomSyncMessage;
import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.RoomStatus;
import com.hcmute.lovestream.service.watchtogether.RoomSessionTracker;
import com.hcmute.lovestream.service.watchtogether.WatchTogetherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomSyncControllerTest {

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Mock
    private WatchTogetherService watchTogetherService;

    @Mock
    private RoomSessionTracker roomSessionTracker;

    @InjectMocks
    private RoomSyncController roomSyncController;

    @Test
    void syncVideo_shouldIgnorePlaybackFromViewer() {
        RoomSyncMessage message = RoomSyncMessage.builder()
                .roomCode("ROOM12345")
                .action("PLAY")
                .currentTime(10.0)
                .build();

        Principal viewer = () -> "viewer@lovestream.local";
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId("session-1");

        when(watchTogetherService.isUserHost("ROOM12345", "viewer@lovestream.local")).thenReturn(false);

        roomSyncController.syncVideo(message, viewer, accessor);

        verify(watchTogetherService, never()).applyHostPlaybackAction(anyString(), anyString(), anyString(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(RoomSyncMessage.class));
    }

    @Test
    void syncVideo_shouldPersistSyncTimeWithoutBroadcast() {
        RoomSyncMessage message = RoomSyncMessage.builder()
                .roomCode("ROOM12345")
                .action("SYNC_TIME")
                .currentTime(33.0)
                .build();

        Principal host = () -> "host@lovestream.local";
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId("session-1");

        when(watchTogetherService.isUserHost("ROOM12345", "host@lovestream.local")).thenReturn(true);

        roomSyncController.syncVideo(message, host, accessor);

        verify(watchTogetherService).updateCurrentVideoTime("ROOM12345", "host@lovestream.local", 33.0);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(RoomSyncMessage.class));
    }

    @Test
    void syncVideo_shouldBroadcastJoinOnlyOnFirstSession() {
        RoomSyncMessage message = RoomSyncMessage.builder()
                .roomCode("room12345")
                .action("join")
                .build();

        Principal viewer = () -> "viewer@lovestream.local";
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId("session-join");

        when(roomSessionTracker.registerSession("session-join", "ROOM12345", "viewer@lovestream.local")).thenReturn(true);
        when(watchTogetherService.countActiveParticipants("ROOM12345")).thenReturn(3L);
        when(watchTogetherService.getRoomStatus("ROOM12345")).thenReturn(RoomStatus.PLAYING.name());
        when(watchTogetherService.getCurrentVideoTime("ROOM12345")).thenReturn(88.0);

        roomSyncController.syncVideo(message, viewer, accessor);

        verify(watchTogetherService).markParticipantConnected("ROOM12345", "viewer@lovestream.local");
        ArgumentCaptor<RoomSyncMessage> captor = ArgumentCaptor.forClass(RoomSyncMessage.class);
        verify(messagingTemplate).convertAndSend(anyString(), captor.capture());

        RoomSyncMessage broadcast = captor.getValue();
        assertEquals("JOIN", broadcast.getAction());
        assertEquals(3L, broadcast.getCurrentParticipants());
        assertEquals(88.0, broadcast.getCurrentTime());
    }
}
