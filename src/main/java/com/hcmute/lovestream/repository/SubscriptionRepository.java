package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Subscription;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    Optional<Subscription> findTopByUserAndStatusOrderByEndDateDesc(User user, SubscriptionStatus status);
    boolean existsByUserAndStatus(User user, SubscriptionStatus status);

    boolean existsByUser_IdAndStatusAndEndDateAfter(String userId, SubscriptionStatus status, LocalDateTime now);


}
