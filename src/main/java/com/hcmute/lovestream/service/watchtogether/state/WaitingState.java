package com.hcmute.lovestream.service.watchtogether.state;

import com.hcmute.lovestream.entity.Room;
import com.hcmute.lovestream.entity.enums.RoomStatus;
import org.springframework.stereotype.Component;

/**
 * State Pattern — WaitingState (Concrete State).
 *
 * Biểu diễn trạng thái WAITING: Phòng đang chờ, Host chưa bấm phát.
 * - play()  → hợp lệ: chuyển sang PLAYING
 * - pause() → vô nghĩa khi chưa phát → ném exception
 * - seek()  → vô nghĩa khi chưa phát → ném exception
 * - stop()  → đã ở trạng thái chờ rồi → không làm gì (idempotent)
 */
@Component
public class WaitingState implements RoomState {

    @Override
    public void play(Room room) {
        room.setStatus(RoomStatus.PLAYING);
    }

    @Override
    public void pause(Room room) {
        throw new IllegalStateException("Không thể tạm dừng khi phòng chưa bắt đầu phát.");
    }

    @Override
    public void seek(Room room, double currentTime) {
        throw new IllegalStateException("Không thể tua khi phòng chưa bắt đầu phát.");
    }

    @Override
    public void stop(Room room) {
        // Đã ở WAITING rồi, không cần thay đổi gì (idempotent)
    }
}
