package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByUserOrderByCreatedAtDesc(User user);
}
