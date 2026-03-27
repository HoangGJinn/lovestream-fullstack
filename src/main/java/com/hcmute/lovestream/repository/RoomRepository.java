package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Room;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

	@EntityGraph(attributePaths = {"videoContent", "videoContent.mediaAssets", "host"})
	@Query("select r from Room r where r.isPrivate = false order by r.createdAt desc")
	List<Room> findLatestPublicRooms();

	@EntityGraph(attributePaths = {"videoContent", "videoContent.mediaAssets", "host"})
	List<Room> findByHost_EmailOrderByCreatedAtDesc(String hostEmail);


	Optional<Room> findByRoomCode(String roomCode);

	boolean existsByRoomCode(String roomCode);
}
