package com.hcmute.lovestream.service.watchtogether.state;

import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.entity.enums.RoomStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * State Pattern — RoomStateContext (Context).
 *
 * Đây là trái tim của State Pattern. Context có nhiệm vụ:
 * 1. Ánh xạ trạng thái hiện tại của Room (RoomStatus enum từ DB)
 *    sang đối tượng State tương ứng.
 * 2. Ủy quyền lệnh playback (PLAY, PAUSE, SEEK, STOP) xuống cho
 *    đối tượng State đang hoạt động.
 * 3. Cập nhật thời gian video sau khi State thay đổi trạng thái.
 *
 * Nhờ cơ chế này, WatchTogetherService không cần switch-case cứng nhắc nữa.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoomStateContext {

    private final WaitingState waitingState;
    private final PlayingState playingState;
    private final PausedState pausedState;

    /**
     * Ánh xạ RoomStatus từ DB sang đối tượng State phù hợp.
     */
    private RoomState resolveState(RoomStatus status) {
        return switch (status) {
            case WAITING -> waitingState;
            case PLAYING -> playingState;
            case PAUSED  -> pausedState;
        };
    }

    /**
     * Điểm vào duy nhất để xử lý lệnh playback từ Host.
     * Ánh xạ action string → gọi phương thức đúng trên State hiện tại của Room.
     *
     * @param room        Thực thể phòng (sẽ bị chỉnh sửa trực tiếp trong hàm này)
     * @param action      Chuỗi lệnh (PLAY / PAUSE / SEEK / STOP), đã được chuẩn hóa UPPER_CASE
     * @param currentTime Thời gian video hiện tại (giây), dùng cho SEEK
     */
    public void applyAction(Room room, String action, double currentTime) {
        RoomState currentState = resolveState(room.getStatus());

        log.debug("[RoomState] Room={} | CurrentState={} | Action={} | Time={}s",
                room.getRoomCode(), room.getStatus(), action, currentTime);

        switch (action) {
            case "PLAY"  -> {
                currentState.play(room);
                room.setCurrentVideoTime(currentTime);
            }
            case "PAUSE" -> {
                currentState.pause(room);
                room.setCurrentVideoTime(currentTime);
            }
            case "SEEK"  -> currentState.seek(room, currentTime);
            case "STOP"  -> {
                currentState.stop(room);
                room.setCurrentVideoTime(0.0);
            }
            default -> throw new IllegalArgumentException("Action không hợp lệ: " + action);
        }

        log.debug("[RoomState] Room={} | NewState={}", room.getRoomCode(), room.getStatus());
    }
}
