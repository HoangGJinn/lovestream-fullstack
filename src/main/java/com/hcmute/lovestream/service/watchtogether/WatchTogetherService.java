package com.hcmute.lovestream.service.watchtogether;

import com.hcmute.lovestream.dto.request.CreateRoomRequest;
import com.hcmute.lovestream.dto.response.WatchRoomCardResponse;
import com.hcmute.lovestream.dto.response.WatchRoomStateResponse;
import com.hcmute.lovestream.entity.Genre;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class WatchTogetherService {

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final VideoContentRepository videoContentRepository;
    private final UserRepository userRepository;
    private final ServicePlanService servicePlanService;

    @Transactional(readOnly = true)
    public List<VideoContent> getCreateRoomMovieOptions() {
        return videoContentRepository
                .findAll(PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "releaseYear")))
                .getContent();
    }

    // Lấy danh sách các phòng công khai mới nhất, giới hạn 30 phòng
    @Transactional(readOnly = true)
    public List<WatchRoomCardResponse> listPublicRooms() {
        return roomRepository.findLatestPublicRooms().stream().limit(30)
                .map(this::toCard)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<WatchRoomCardResponse> findRoomByCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            return Optional.empty();
        }
        return roomRepository.findByRoomCode(roomCode.trim().toUpperCase(Locale.ROOT)).map(this::toCard);
    }

    // tìm kiếm phòng theo mã phòng
    @Transactional(readOnly = true)
    public Optional<Room> findRoomEntityByCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            return Optional.empty();
        }
        return roomRepository.findByRoomCode(roomCode.trim().toUpperCase(Locale.ROOT));
    }

    // Lấy danh sách các phòng do user này tạo ra
    @Transactional(readOnly = true)
    public List<WatchRoomCardResponse> listRoomsHostedBy(String userEmail) {
        return roomRepository.findByHost_EmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toCard)
                .toList();
    }
// Lấy thông tin chi tiết của phòng, bao gồm cả vai trò của user trong phòng đó
    @Transactional(readOnly = true)
    public WatchRoomStateResponse getRoomState(String roomCode, String userEmail) {
        assertActiveSubscription(userEmail);

        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại"));

        RoomParticipant participant = roomParticipantRepository.findByRoom_IdAndUser_Email(room.getId(), userEmail)
                .orElseThrow(() -> new IllegalStateException("Bạn không phải là thành viên của phòng này"));

        return new WatchRoomStateResponse(
                room.getRoomCode(),
                room.getRoomName(),
                room.getStatus().name(),
                participant.getRole() == RoomRole.HOST,
                room.isPrivate()
        );
    }

    // tao phòng
    @Transactional
    public Room createRoom(String userEmail, CreateRoomRequest request) {
        assertActiveSubscription(userEmail);

        User host = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User khong ton tai"));

        VideoContent video = videoContentRepository.findById(request.getVideoContentId())
                .orElseThrow(() -> new IllegalArgumentException("Noi dung phim khong ton tai"));

        String roomCode = generateUniqueRoomCode();
        int maxParticipants = request.getMaxParticipants() == null ? 10 : request.getMaxParticipants();
        boolean privateRoom = Boolean.TRUE.equals(request.getPrivateRoom());
        String roomPassword = normalizePassword(request.getPassword(), privateRoom);
        Room room = Room.builder()
                .roomName(normalizeRoomName(request.getRoomName()))
                .roomCode(roomCode)
                .isPrivate(privateRoom)
                .password(roomPassword)
                .maxParticipants(maxParticipants)
                .status(RoomStatus.WAITING)
                .host(host)
                .videoContent(video)
                .build();

        Room savedRoom = roomRepository.save(room);

        RoomParticipant hostParticipant = RoomParticipant.builder()
                .room(savedRoom)
                .user(host)
                .role(RoomRole.HOST)
                .connectionStatus(ConnectionStatus.CONNECTED)
                .build();
        roomParticipantRepository.save(hostParticipant);

        return savedRoom;
    }

    @Transactional
    public Room joinRoom(String roomCode, String userEmail) {
        return joinRoom(roomCode, userEmail, null);
    }

    @Transactional
    public Room joinRoom(String roomCode, String userEmail, String rawPassword) {
        assertActiveSubscription(userEmail);

        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User khong ton tai"));

        if (roomParticipantRepository.existsByRoom_IdAndUser_Id(room.getId(), user.getId())) {
            return room;
        }

        long participantCount = roomParticipantRepository.countByRoom_Id(room.getId());
        if (participantCount >= room.getMaxParticipants()) {
            throw new IllegalStateException("Phong da day");
        }

        if (room.isPrivate() && !isPasswordValid(room.getPassword(), rawPassword)) {
            throw new IllegalArgumentException("Mat khau phong khong dung");
        }

        RoomParticipant participant = RoomParticipant.builder()
                .room(room)
                .user(user)
                .role(RoomRole.VIEWER)
                .connectionStatus(ConnectionStatus.CONNECTED)
                .build();
        roomParticipantRepository.save(participant);

        return room;
    }

    @Transactional
    public Room startRoom(String roomCode, String hostEmail) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại"));
        validateHost(room, hostEmail);
        room.setStatus(RoomStatus.PLAYING);
        return roomRepository.save(room);
    }

    @Transactional
    public Room stopRoom(String roomCode, String hostEmail) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại"));
        validateHost(room, hostEmail);
        room.setStatus(RoomStatus.WAITING);
        return roomRepository.save(room);
    }

    private WatchRoomCardResponse toCard(Room room) {
        long participantCount = roomParticipantRepository.countByRoom_Id(room.getId());
        String rawCategory = room.getVideoContent().getGenres().stream()
                .map(Genre::getName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Khac");
        String category = mapGenreLabel(rawCategory);

        boolean live = room.getStatus() == RoomStatus.PLAYING;
        String statusText = switch (room.getStatus()) {
            case PLAYING -> "Đang chiếu";
            case PAUSED -> "Tạm dừng";
            case WAITING -> "Đang chờ";
        };

        return new WatchRoomCardResponse(
                room.getRoomName(),
                room.getRoomCode(),
                room.isPrivate(),
                room.getVideoContent().getId(),
                room.getVideoContent().getTitle(),
                room.getVideoContent().getPosterUrl(),
                category,
                statusText,
                live,
                participantCount,
                room.getMaxParticipants(),
                Optional.ofNullable(room.getHost()).map(User::getFullName).orElse("Unknown"),
                formatDateTime(room.getCreatedAt())
        );
    }

    private String formatDateTime(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "Vừa tạo";
        }
        return CREATED_AT_FORMAT.format(createdAt);
    }

    private String normalizeRoomName(String value) {
        if (value == null) {
            return "Phòng xem chung";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "Phòng xem chung" : trimmed;
    }

    private String normalizePassword(String value, boolean privateRoom) {
        if (!privateRoom) {
            return null;
        }
        if (value == null) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu cho phòng riêng tư");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu cho phòng riêng tư");
        }
        return trimmed;
    }


    // FIX LẠI CALL THE LOAI TU Database
    private String mapGenreLabel(String genreName) {
        String normalized = genreName.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "action" -> "Hanh dong";
            case "horror" -> "Kinh di";
            case "drama", "romance" -> "Tinh cam";
            case "anime", "animation" -> "Anime";
            default -> genreName;
        };
    }

    private void assertActiveSubscription(String userEmail) {
        if (!servicePlanService.hasActiveSubscription(userEmail)) {
            throw new IllegalStateException("Bạn cần có gói dịch vụ hợp lệ để sử dụng tính năng này");
        }
    }

    private boolean isPasswordValid(String savedPassword, String rawPassword) {
        String expected = savedPassword == null ? "" : savedPassword.trim();
        String actual = rawPassword == null ? "" : rawPassword.trim();
        return !expected.isEmpty() && expected.equals(actual);
    }

    private void validateHost(Room room, String hostEmail) {
        if (room.getHost() == null || room.getHost().getEmail() == null || !room.getHost().getEmail().equals(hostEmail)) {
            throw new IllegalStateException("Chỉ chủ phòng mới có quyền thực hiện thao tác này");
        }
    }

    private String generateUniqueRoomCode() {
        for (int i = 0; i < 10; i++) {
            String code = randomCode(8);
            if (!roomRepository.existsByRoomCode(code)) {
                return code;
            }
        }
        return randomCode(12);
    }

    private String randomCode(int size) {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(size);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < size; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}



