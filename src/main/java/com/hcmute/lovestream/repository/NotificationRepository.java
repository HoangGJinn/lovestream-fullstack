package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Notification;
import com.hcmute.lovestream.entity.enums.UserNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
	List<Notification> findByUser_IdAndStatusInOrderBySentAtDesc(
			String userId,
			List<UserNotificationStatus> statuses
	);

	Optional<Notification> findByIdAndUser_Id(String id, String userId);

	long countByUser_IdAndStatus(String userId, UserNotificationStatus status);




	boolean existsByUser_IdAndDedupeKey(String userId, String dedupeKey);
}
