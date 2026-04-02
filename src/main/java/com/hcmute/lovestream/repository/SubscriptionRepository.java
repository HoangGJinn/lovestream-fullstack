package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Subscription;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    Optional<Subscription> findTopByUserAndStatusOrderByEndDateDesc(User user, SubscriptionStatus status);
    Optional<Subscription> findTopByUserAndStatusAndEndDateAfterOrderByEndDateDesc(User user,
                                                                                     SubscriptionStatus status,
                                                                                     LocalDateTime now);
    boolean existsByUserAndStatus(User user, SubscriptionStatus status);

    boolean existsByUser_IdAndStatusAndEndDateAfter(String userId, SubscriptionStatus status, LocalDateTime now);

    @EntityGraph(attributePaths = {"user", "plan"})
    List<Subscription> findByStatusAndEndDateGreaterThanEqualAndEndDateLessThan(
            SubscriptionStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}
