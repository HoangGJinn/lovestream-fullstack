package com.hcmute.lovestream.service.watchtogether.state;

import com.hcmute.lovestream.entity.Room;

/**
 * State Pattern — Interface RoomState (State Interface).
 *
 * Định nghĩa hợp đồng chung cho tất cả các trạng thái của phòng xem chung.
 * Context (RoomStateContext) sẽ ủy quyền mọi lệnh playback xuống cho
 * đối tượng State hiện tại thay vì dùng switch-case.
 *
 * Theo đúng gợi ý trong tài liệu:
 *   - play(Room)
 *   - pause(Room)
 *   - seek(Room, double time)
 *   - stop(Room)
 */
public interface RoomState {

    /**
     * Xử lý lệnh PLAY từ Host.
     * @param room Thực thể phòng hiện tại (Context sẽ lưu lại sau khi State chỉnh sửa)
     */
    void play(Room room);

    /**
     * Xử lý lệnh PAUSE từ Host.
     * @param room Thực thể phòng hiện tại
     */
    void pause(Room room);

    /**
     * Xử lý lệnh SEEK (tua video) từ Host.
     * @param room        Thực thể phòng hiện tại
     * @param currentTime Vị trí thời gian mới (giây) muốn tua đến
     */
    void seek(Room room, double currentTime);

    /**
     * Xử lý lệnh STOP từ Host (dừng phim, về trạng thái chờ).
     * @param room Thực thể phòng hiện tại
     */
    void stop(Room room);
}
