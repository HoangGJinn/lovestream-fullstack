package com.hcmute.lovestream.service.watchtogether.state;

import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.entity.enums.RoomStatus;
import org.springframework.stereotype.Component;

/**
 * State Pattern — PausedState (Concrete State).
 *
 * Biểu diễn trạng thái PAUSED: Phim đang tạm dừng.
 * - play()  → hợp lệ: tiếp tục phát, chuyển sang PLAYING
 * - pause() → đã tạm dừng rồi → không làm gì (idempotent)
 * - seek()  → hợp lệ: giữ PAUSED, cập nhật vị trí thời gian mới
 * - stop()  → hợp lệ: chuyển về WAITING
 */
@Component
public class PausedState implements RoomState {

    @Override
    public void play(Room room) {
        room.setStatus(RoomStatus.PLAYING);
    }

    @Override
    public void pause(Room room) {
        // Đã tạm dừng rồi, không cần thay đổi gì (idempotent)
    }

    @Override
    public void seek(Room room, double currentTime) {
        // Giữ nguyên trạng thái PAUSED, chỉ cập nhật thời gian
        room.setCurrentVideoTime(currentTime);
    }

    @Override
    public void stop(Room room) {
        room.setStatus(RoomStatus.WAITING);
    }
}
