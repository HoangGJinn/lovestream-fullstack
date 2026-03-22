package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.PasswordChangeBackupToken;
import com.hcmute.lovestream.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordChangeBackupTokenRepository extends JpaRepository<PasswordChangeBackupToken, Long> {
    Optional<PasswordChangeBackupToken> findByToken(String token);
    void deleteByUser(User user);
}
