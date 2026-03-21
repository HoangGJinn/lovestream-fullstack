package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
}
