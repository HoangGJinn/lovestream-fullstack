package com.hcmute.lovestream.service.user;

import com.hcmute.lovestream.entity.AccountDeletionToken;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import com.hcmute.lovestream.entity.enums.UserStatus;
import com.hcmute.lovestream.repository.AccountDeletionTokenRepository;
import com.hcmute.lovestream.repository.RefreshTokenRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final AccountDeletionTokenRepository accountDeletionTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;

    @Value("${app.domain:http://localhost:8080}")
    private String appDomain;

    @Transactional
    public void requestAccountDeletion(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung"));

        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Chi tai khoan khach hang moi duoc phep xoa tai khoan");
        }

        if (user.getStatus() == UserStatus.REMOVED) {
            throw new IllegalArgumentException("Tai khoan da o trang thai da xoa");
        }

        if (user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.LOCKED) {
            throw new IllegalArgumentException("Tai khoan dang bi khoa, khong the thuc hien thao tac nay");
        }

        accountDeletionTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        AccountDeletionToken deletionToken = AccountDeletionToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .used(false)
                .build();
        accountDeletionTokenRepository.save(deletionToken);

        String confirmLink = appDomain + "/account/delete/confirm?token=" + token;
        emailService.sendAccountDeletionConfirmationEmail(user.getEmail(), confirmLink);
    }

    @Transactional
    public DeletionResult confirmAccountDeletion(String token) {
        AccountDeletionToken deletionToken = accountDeletionTokenRepository.findByToken(token).orElse(null);
        if (deletionToken == null || deletionToken.isUsed()) {
            return DeletionResult.INVALID;
        }

        if (deletionToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return DeletionResult.EXPIRED;
        }

        User user = deletionToken.getUser();
        if (user.getStatus() != UserStatus.REMOVED) {
            user.setStatus(UserStatus.REMOVED);
            user.setLockReason("Tai khoan da duoc nguoi dung yeu cau xoa");
            userRepository.save(user);
            refreshTokenRepository.revokeAllUserTokens(user.getId());
        }

        deletionToken.setUsed(true);
        accountDeletionTokenRepository.save(deletionToken);
        return DeletionResult.SUCCESS;
    }

    public enum DeletionResult {
        SUCCESS,
        INVALID,
        EXPIRED
    }
}
