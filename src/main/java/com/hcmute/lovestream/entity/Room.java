package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "room_name", nullable = false)
    private String roomName;

    @Column(name = "room_code", unique = true, nullable = false)
    private String roomCode; // Mã phòng hoặc link mời

    @Column(name = "is_private")
    @Builder.Default
    private boolean isPrivate = false;

    @Column(name = "max_participants")
    @Builder.Default
    private int maxParticipants = 10; // Giới hạn số người xem chung

    // Trạng thái hiện tại của phòng (Mặc định là đang chờ)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomStatus status = RoomStatus.WAITING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Quan hệ n-1: Chủ phòng (Người tạo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User host;

    // Quan hệ n-1: Phim đang được xem trong phòng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_content_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VideoContent videoContent;

    // Quan hệ 1-n: Danh sách người tham gia phòng này
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<RoomParticipant> participants = new ArrayList<>();
}