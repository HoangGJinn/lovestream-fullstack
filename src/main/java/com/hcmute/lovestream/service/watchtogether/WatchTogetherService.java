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
        return roomRepository.findByRoomCode(normalizeRoomCode(roomCode)).map(this::toCard);
    }

    // tìm kiếm phòng theo mã phòng
    @Transactional(readOnly = true)
    public Optional<Room> findRoomEntityByCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            return Optional.empty();
        }
        return roomRepository.findByRoomCode(normalizeRoomCode(roomCode));
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

        Room room = findRoomByCodeOrThrow(roomCode);
        RoomParticipant participant = roomParticipantRepository.findByRoom_IdAndUser_Email(room.getId(), userEmail)
                .orElseThrow(() -> new IllegalStateException("Bạn không phải là thành viên của phòng này"));

        return buildRoomState(room, participant);
    }

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
                .currentVideoTime(0.0)
                .host(host)
                .videoContent(video)
                .build();

        Room savedRoom = roomRepository.save(room);

        RoomParticipant hostParticipant = RoomParticipant.builder()
                .room(savedRoom)
                .user(host)
                .role(RoomRole.HOST)
                // Presence is controlled by WebSocket JOIN/DISCONNECT events.
                .connectionStatus(ConnectionStatus.DISCONNECTED)
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

        Room room = findRoomByCodeOrThrow(roomCode);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User khong ton tai"));

        Optional<RoomParticipant> existingParticipant = roomParticipantRepository.findByRoom_IdAndUser_Id(room.getId(), user.getId());
        if (existingParticipant.isPresent()) {
            return room;
        }

        long participantCount = countConnectedParticipantsByRoomId(room.getId());
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
                .connectionStatus(ConnectionStatus.DISCONNECTED)
                .build();
        roomParticipantRepository.save(participant);

        return room;
    }

    @Transactional
    public Room startRoom(String roomCode, String hostEmail) {
        Room room = findRoomByCodeOrThrow(roomCode);
        validateHost(room, hostEmail);
        room.setStatus(RoomStatus.PLAYING);
        return roomRepository.save(room);
    }

    @Transactional
    public Room stopRoom(String roomCode, String hostEmail) {
        Room room = findRoomByCodeOrThrow(roomCode);
        validateHost(room, hostEmail);
        room.setStatus(RoomStatus.WAITING);
        return roomRepository.save(room);
    }

    @Transactional
    public void markParticipantConnected(String roomCode, String userEmail) {
        Room room = findRoomByCodeOrThrow(roomCode);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User khong ton tai"));

        RoomParticipant participant = roomParticipantRepository.findByRoom_IdAndUser_Id(room.getId(), user.getId())
                .orElseThrow(() -> new IllegalStateException("Ban chua tham gia phong nay"));

        if (participant.getConnectionStatus() != ConnectionStatus.CONNECTED) {
            participant.setConnectionStatus(ConnectionStatus.CONNECTED);
            roomParticipantRepository.save(participant);
        }
    }

    @Transactional
    public void markParticipantDisconnected(String roomCode, String userEmail) {
        Room room = findRoomByCodeOrThrow(roomCode);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User khong ton tai"));

        RoomParticipant participant = roomParticipantRepository.findByRoom_IdAndUser_Id(room.getId(), user.getId())
                .orElseThrow(() -> new IllegalStateException("Ban chua tham gia phong nay"));

        if (participant.getConnectionStatus() != ConnectionStatus.DISCONNECTED) {
            participant.setConnectionStatus(ConnectionStatus.DISCONNECTED);
            roomParticipantRepository.save(participant);
        }
    }

    @Transactional(readOnly = true)
    public long countActiveParticipants(String roomCode) {
        Room room = findRoomByCodeOrThrow(roomCode);
        return countConnectedParticipantsByRoomId(room.getId());
    }

    @Transactional(readOnly = true)
    public boolean isUserHost(String roomCode, String userEmail) {
        Room room = findRoomByCodeOrThrow(roomCode);
        return room.getHost() != null
                && room.getHost().getEmail() != null
                && room.getHost().getEmail().equals(userEmail);
    }

    @Transactional
    public Room applyHostPlaybackAction(String roomCode, String hostEmail, String action, Double currentTime) {
        Room room = findRoomByCodeOrThrow(roomCode);
        validateHost(room, hostEmail);

        String normalizedAction = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        switch (normalizedAction) {
            case "PLAY" -> room.setStatus(RoomStatus.PLAYING);
            case "PAUSE" -> room.setStatus(RoomStatus.PAUSED);
            case "SEEK" -> {
                // Keep current status, only update time.
            }
            case "STOP" -> room.setStatus(RoomStatus.WAITING);
            default -> throw new IllegalArgumentException("Action khong hop le");
        }

        room.setCurrentVideoTime(normalizeCurrentTime(currentTime));
        return roomRepository.save(room);
    }

    @Transactional
    public Room updateCurrentVideoTime(String roomCode, String hostEmail, Double currentTime) {
        Room room = findRoomByCodeOrThrow(roomCode);
        validateHost(room, hostEmail);
        room.setCurrentVideoTime(normalizeCurrentTime(currentTime));
        return roomRepository.save(room);
    }

    @Transactional
    public Room forceStopRoom(String roomCode) {
        Room room = findRoomByCodeOrThrow(roomCode);
        room.setStatus(RoomStatus.WAITING);
        return roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public double getCurrentVideoTime(String roomCode) {
        Room room = findRoomByCodeOrThrow(roomCode);
        return normalizeCurrentTime(room.getCurrentVideoTime());
    }

    @Transactional(readOnly = true)
    public String getRoomStatus(String roomCode) {
        Room room = findRoomByCodeOrThrow(roomCode);
        return room.getStatus().name();
    }

    private WatchRoomStateResponse buildRoomState(Room room, RoomParticipant participant) {
        long participantCount = countConnectedParticipantsByRoomId(room.getId());
        return new WatchRoomStateResponse(
                room.getRoomCode(),
                room.getRoomName(),
                room.getStatus().name(),
                participant.getRole() == RoomRole.HOST,
                room.isPrivate(),
                participantCount,
                normalizeCurrentTime(room.getCurrentVideoTime())
        );
    }

    private WatchRoomCardResponse toCard(Room room) {
        long participantCount = countConnectedParticipantsByRoomId(room.getId());
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
                formatDateTime(room.getCreatedAt()),
                room.getPassword()
        );
    }

    private String formatDateTime(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "Vua tao";
        }
        return CREATED_AT_FORMAT.format(createdAt);
    }

    private Room findRoomByCodeOrThrow(String roomCode) {
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        return roomRepository.findByRoomCode(normalizedRoomCode)
                .orElseThrow(() -> new IllegalArgumentException("Phong khong ton tai"));
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalArgumentException("Ma phong khong hop le");
        }
        return roomCode.trim().toUpperCase(Locale.ROOT);
    }

    private long countConnectedParticipantsByRoomId(String roomId) {
        return roomParticipantRepository.countByRoom_IdAndConnectionStatus(roomId, ConnectionStatus.CONNECTED);
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

    private double normalizeCurrentTime(Double currentTime) {
        if (currentTime == null || currentTime.isNaN() || currentTime < 0) {
            return 0.0;
        }
        return currentTime;
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
