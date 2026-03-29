package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.AccountDeletionToken;
import com.hcmute.lovestream.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountDeletionTokenRepository extends JpaRepository<AccountDeletionToken, Long> {
    Optional<AccountDeletionToken> findByToken(String token);
    void deleteByUser(User user);
}
