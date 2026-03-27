package com.hcmute.lovestream.controller.websocket;

import com.hcmute.lovestream.dto.websocket.RoomSyncMessage;
import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.entity.enums.RoomStatus;
import com.hcmute.lovestream.service.watchtogether.RoomSessionTracker;
import com.hcmute.lovestream.service.watchtogether.WatchTogetherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomWebSocketEventListenerTest {

    @Mock
    private RoomSessionTracker roomSessionTracker;

    @Mock
    private WatchTogetherService watchTogetherService;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @InjectMocks
    private RoomWebSocketEventListener listener;

    @Test
    void handleWebSocketDisconnectListener_shouldAutoStopWhenHostLeavesLastSession() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeader(SimpMessageHeaderAccessor.SESSION_ID_HEADER, "session-1")
                .build();

        SessionDisconnectEvent event = org.mockito.Mockito.mock(SessionDisconnectEvent.class);
        when(event.getMessage()).thenReturn(message);

        RoomSessionTracker.DisconnectInfo disconnectInfo = new RoomSessionTracker.DisconnectInfo(
                "ROOM12345",
                "host@lovestream.local",
                true
        );

        Room stoppedRoom = Room.builder()
                .roomCode("ROOM12345")
                .status(RoomStatus.WAITING)
                .currentVideoTime(55.0)
                .build();

        when(roomSessionTracker.unregisterSession("session-1")).thenReturn(Optional.of(disconnectInfo));
        when(watchTogetherService.isUserHost("ROOM12345", "host@lovestream.local")).thenReturn(true);
        when(watchTogetherService.forceStopRoom("ROOM12345")).thenReturn(stoppedRoom);
        when(watchTogetherService.countActiveParticipants("ROOM12345")).thenReturn(1L);

        listener.handleWebSocketDisconnectListener(event);

        verify(watchTogetherService).markParticipantDisconnected("ROOM12345", "host@lovestream.local");
        verify(watchTogetherService).forceStopRoom("ROOM12345");

        ArgumentCaptor<RoomSyncMessage> payloadCaptor = ArgumentCaptor.forClass(RoomSyncMessage.class);
        verify(messagingTemplate).convertAndSend(anyString(), payloadCaptor.capture());

        RoomSyncMessage payload = payloadCaptor.getValue();
        assertEquals("LEAVE", payload.getAction());
        assertEquals("WAITING", payload.getStatus());
        assertEquals(55.0, payload.getCurrentTime());
        assertEquals(1L, payload.getCurrentParticipants());
    }

    @Test
    void handleWebSocketDisconnectListener_shouldIgnoreIfNotLastSession() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeader(SimpMessageHeaderAccessor.SESSION_ID_HEADER, "session-2")
                .build();

        SessionDisconnectEvent event = org.mockito.Mockito.mock(SessionDisconnectEvent.class);
        when(event.getMessage()).thenReturn(message);

        RoomSessionTracker.DisconnectInfo disconnectInfo = new RoomSessionTracker.DisconnectInfo(
                "ROOM12345",
                "viewer@lovestream.local",
                false
        );
        when(roomSessionTracker.unregisterSession("session-2")).thenReturn(Optional.of(disconnectInfo));

        listener.handleWebSocketDisconnectListener(event);

        verify(watchTogetherService, never()).markParticipantDisconnected(anyString(), anyString());
        verify(messagingTemplate, never()).convertAndSend(anyString(), org.mockito.ArgumentMatchers.any(RoomSyncMessage.class));
    }
}
