package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, String> {
}
