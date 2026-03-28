package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Notification;
import com.hcmute.lovestream.entity.enums.UserNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
	List<Notification> findByUser_IdAndStatusNotOrderBySentAtDesc(String userId, UserNotificationStatus status);

	List<Notification> findByUser_IdAndStatusOrderBySentAtDesc(String userId, UserNotificationStatus status);

	long countByUser_IdAndStatus(String userId, UserNotificationStatus status);

	Optional<Notification> findByIdAndUser_Id(String id, String userId);

	boolean existsByUser_IdAndDedupeKey(String userId, String dedupeKey);
}
