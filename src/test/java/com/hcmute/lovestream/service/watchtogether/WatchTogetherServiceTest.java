package com.hcmute.lovestream.service.watchtogether;

import com.hcmute.lovestream.dto.request.CreateRoomRequest;
import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.entity.RoomParticipant;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.ConnectionStatus;
import com.hcmute.lovestream.entity.enums.RoomRole;
import com.hcmute.lovestream.entity.enums.RoomStatus;
import com.hcmute.lovestream.repository.RoomParticipantRepository;
import com.hcmute.lovestream.repository.RoomRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.repository.VideoContentRepository;
import com.hcmute.lovestream.service.plan.ServicePlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchTogetherServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private VideoContentRepository videoContentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServicePlanService servicePlanService;

    @InjectMocks
    private WatchTogetherService watchTogetherService;

    private CreateRoomRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateRoomRequest();
        request.setRoomName("Demo room");
        request.setVideoContentId("video-1");
        request.setPrivateRoom(Boolean.FALSE);
        request.setMaxParticipants(20);
    }

    @Test
    void createRoom_shouldSeedHostParticipantAndPersistRoom() {
        User host = User.builder().id("user-1").email("demo@lovestream.local").fullName("Demo User").build();
        VideoContent video = org.mockito.Mockito.mock(VideoContent.class);

        when(userRepository.findByEmail("demo@lovestream.local")).thenReturn(Optional.of(host));
        when(videoContentRepository.findById("video-1")).thenReturn(Optional.of(video));
        when(servicePlanService.hasActiveSubscription("demo@lovestream.local")).thenReturn(true);
        when(roomRepository.existsByRoomCode(anyString())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId("room-1");
            return room;
        });

        Room created = watchTogetherService.createRoom("demo@lovestream.local", request);

        assertNotNull(created.getRoomCode());
        assertTrue(created.getRoomCode().length() >= 8);
        assertEquals(RoomStatus.WAITING, created.getStatus());
        assertEquals("Demo room", created.getRoomName());
        assertEquals(0.0, created.getCurrentVideoTime());

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository, times(1)).save(roomCaptor.capture());
        assertEquals("Demo room", roomCaptor.getValue().getRoomName());
        verify(roomParticipantRepository, times(1)).save(any());
    }

    @Test
    void joinRoom_shouldKeepExistingParticipantDisconnectedUntilWebSocketJoin() {
        String roomCode = "ROOM12345";
        String userEmail = "viewer@lovestream.local";

        User host = User.builder().id("host-1").email("host@lovestream.local").build();
        User viewer = User.builder().id("user-1").email(userEmail).build();

        Room room = Room.builder()
                .id("room-1")
                .roomCode(roomCode)
                .host(host)
                .status(RoomStatus.WAITING)
                .maxParticipants(10)
                .build();

        RoomParticipant participant = RoomParticipant.builder()
                .room(room)
                .user(viewer)
                .role(RoomRole.VIEWER)
                .connectionStatus(ConnectionStatus.DISCONNECTED)
                .build();

        when(servicePlanService.hasActiveSubscription(userEmail)).thenReturn(true);
        when(roomRepository.findByRoomCode(roomCode)).thenReturn(Optional.of(room));
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(viewer));
        when(roomParticipantRepository.findByRoom_IdAndUser_Id(room.getId(), viewer.getId())).thenReturn(Optional.of(participant));

        Room joined = watchTogetherService.joinRoom(roomCode, userEmail, null);

        assertEquals(room, joined);
        assertEquals(ConnectionStatus.DISCONNECTED, participant.getConnectionStatus());
        verify(roomParticipantRepository, never()).save(participant);
    }

    @Test
    void markParticipantConnected_shouldFlipConnectionStatus() {
        String roomCode = "ROOM12345";
        String userEmail = "viewer@lovestream.local";

        User host = User.builder().id("host-1").email("host@lovestream.local").build();
        User viewer = User.builder().id("user-1").email(userEmail).build();
        Room room = Room.builder().id("room-1").roomCode(roomCode).host(host).build();
        RoomParticipant participant = RoomParticipant.builder()
                .room(room)
                .user(viewer)
                .connectionStatus(ConnectionStatus.DISCONNECTED)
                .build();

        when(roomRepository.findByRoomCode(roomCode)).thenReturn(Optional.of(room));
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(viewer));
        when(roomParticipantRepository.findByRoom_IdAndUser_Id(room.getId(), viewer.getId()))
                .thenReturn(Optional.of(participant));

        watchTogetherService.markParticipantConnected(roomCode, userEmail);

        assertEquals(ConnectionStatus.CONNECTED, participant.getConnectionStatus());
        verify(roomParticipantRepository).save(participant);
    }

    @Test
    void applyHostPlaybackAction_shouldUpdateStatusAndCurrentTime() {
        String roomCode = "ROOM12345";
        String hostEmail = "host@lovestream.local";

        User host = User.builder().id("host-1").email(hostEmail).build();
        Room room = Room.builder()
                .id("room-1")
                .roomCode(roomCode)
                .host(host)
                .status(RoomStatus.WAITING)
                .currentVideoTime(0.0)
                .build();

        when(roomRepository.findByRoomCode(roomCode)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room updated = watchTogetherService.applyHostPlaybackAction(roomCode, hostEmail, "PLAY", 42.5);

        assertEquals(RoomStatus.PLAYING, updated.getStatus());
        assertEquals(42.5, updated.getCurrentVideoTime());
    }

    @Test
    void applyHostPlaybackAction_shouldRejectViewerControl() {
        String roomCode = "ROOM12345";

        User host = User.builder().id("host-1").email("host@lovestream.local").build();
        Room room = Room.builder()
                .id("room-1")
                .roomCode(roomCode)
                .host(host)
                .status(RoomStatus.WAITING)
                .build();

        when(roomRepository.findByRoomCode(roomCode)).thenReturn(Optional.of(room));

        assertThrows(IllegalStateException.class,
                () -> watchTogetherService.applyHostPlaybackAction(roomCode, "viewer@lovestream.local", "PLAY", 20.0));

        verify(roomRepository, never()).save(any(Room.class));
    }
}
