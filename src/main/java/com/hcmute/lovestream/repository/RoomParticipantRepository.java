package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.RoomParticipant;
import com.hcmute.lovestream.entity.enums.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, String> {

	long countByRoom_Id(String roomId);

	boolean existsByRoom_IdAndUser_Id(String roomId, String userId);

	Optional<RoomParticipant> findByRoom_IdAndUser_Email(String roomId, String userEmail);

	Optional<RoomParticipant> findByRoom_IdAndUser_Id(String roomId, String userId);

	@Query("select count(rp) from RoomParticipant rp where rp.room.id = :roomId and rp.connectionStatus = :status")
	long countByRoom_IdAndConnectionStatus(@Param("roomId") String roomId, @Param("status") ConnectionStatus status);

	@Query("select count(rp) from RoomParticipant rp where rp.room.roomCode = :roomCode and rp.connectionStatus = :status")
	long countByRoom_RoomCodeAndConnectionStatus(@Param("roomCode") String roomCode, @Param("status") ConnectionStatus status);
}
