package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.ConnectionStatus;
import com.hcmute.lovestream.entity.enums.RoomRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_participants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Phòng mà người dùng tham gia
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Room room;

    // Người dùng tham gia
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    // Vai trò của người này trong phòng
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomRole role = RoomRole.VIEWER;

    // Trạng thái kết nối (Phục vụ cho việc báo "User X đã rớt mạng")
    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false)
    @Builder.Default
    private ConnectionStatus connectionStatus = ConnectionStatus.CONNECTED;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;
}