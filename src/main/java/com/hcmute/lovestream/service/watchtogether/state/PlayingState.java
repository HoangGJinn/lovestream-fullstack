package com.hcmute.lovestream.service.watchtogether.state;

import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.entity.enums.RoomStatus;
import org.springframework.stereotype.Component;

/**
 * State Pattern — PlayingState (Concrete State).
 *
 * Biểu diễn trạng thái PLAYING: Phim đang phát.
 * - play()  → đã đang phát rồi → không làm gì (idempotent)
 * - pause() → hợp lệ: chuyển sang PAUSED
 * - seek()  → hợp lệ: giữ PLAYING, cập nhật vị trí thời gian mới
 * - stop()  → hợp lệ: chuyển về WAITING
 */
@Component
public class PlayingState implements RoomState {

    @Override
    public void play(Room room) {
        // Đã đang phát rồi, không cần thay đổi gì (idempotent)
    }

    @Override
    public void pause(Room room) {
        room.setStatus(RoomStatus.PAUSED);
    }

    @Override
    public void seek(Room room, double currentTime) {
        // Giữ nguyên trạng thái PLAYING, chỉ cập nhật thời gian
        room.setCurrentVideoTime(currentTime);
    }

    @Override
    public void stop(Room room) {
        room.setStatus(RoomStatus.WAITING);
    }
}
