package com.hcmute.lovestream.service.watchtogether;

import com.hcmute.lovestream.dto.request.CreateRoomRequest;
import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.VideoContent;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository, times(1)).save(roomCaptor.capture());
        assertEquals("Demo room", roomCaptor.getValue().getRoomName());
        verify(roomParticipantRepository, times(1)).save(any());
    }
}

