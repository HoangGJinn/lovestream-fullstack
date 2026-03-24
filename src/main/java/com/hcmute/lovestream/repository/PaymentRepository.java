package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByUserOrderByCreatedAtDesc(User user);
    Optional<Payment> findByTransactionCode(String transactionCode);
    boolean existsByTransactionCode(String transactionCode);
}
