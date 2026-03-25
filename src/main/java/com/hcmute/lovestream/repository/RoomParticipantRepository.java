package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, String> {

	long countByRoom_Id(String roomId);

	boolean existsByRoom_IdAndUser_Id(String roomId, String userId);

	Optional<RoomParticipant> findByRoom_IdAndUser_Email(String roomId, String userEmail);
}
