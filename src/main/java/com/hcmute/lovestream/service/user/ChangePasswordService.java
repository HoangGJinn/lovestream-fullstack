package com.hcmute.lovestream.service.user;

import com.hcmute.lovestream.dto.request.BackupChangePasswordRequest;
import com.hcmute.lovestream.dto.request.ChangePasswordRequest;
import com.hcmute.lovestream.entity.PasswordChangeBackupToken;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.repository.PasswordChangeBackupTokenRepository;
import com.hcmute.lovestream.repository.RefreshTokenRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangePasswordService {

    private final UserRepository userRepository;
    private final PasswordChangeBackupTokenRepository backupTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${server.port:8080}")
    private String serverPort;

    @Transactional
    public void changePasswordByOldPassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("Mật khẩu và nhập lại mật khẩu không trùng nhau");
        }

        updatePasswordAndIssueBackupLink(user, request.getNewPassword());
    }

    @Transactional(readOnly = true)
    public BackupTokenStatus validateBackupToken(String token) {
        PasswordChangeBackupToken backupToken = backupTokenRepository.findByToken(token)
                .orElse(null);

        if (backupToken == null || backupToken.isUsed()) {
            return BackupTokenStatus.INVALID;
        }

        if (backupToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return BackupTokenStatus.EXPIRED;
        }

        return BackupTokenStatus.VALID;
    }

    @Transactional
    public void changePasswordByBackupLink(BackupChangePasswordRequest request) {
        PasswordChangeBackupToken backupToken = backupTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Đường dẫn không hợp lệ"));

        if (backupToken.isUsed()) {
            throw new IllegalArgumentException("Đường dẫn không hợp lệ");
        }

        if (backupToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Đường dẫn đã hết hạn");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("Mật khẩu và nhập lại mật khẩu không trùng nhau");
        }

        User user = backupToken.getUser();
        backupToken.setUsed(true);
        backupTokenRepository.save(backupToken);

        updatePasswordAndIssueBackupLink(user, request.getNewPassword());
    }

    @Transactional
    protected void updatePasswordAndIssueBackupLink(User user, String newRawPassword) {
        user.setPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);

        // Đăng xuất khỏi toàn bộ phiên đăng nhập hiện có sau khi đổi mật khẩu.
        refreshTokenRepository.revokeAllUserTokens(user.getId());

        backupTokenRepository.deleteByUser(user);
        String token = UUID.randomUUID().toString();

        PasswordChangeBackupToken backupToken = PasswordChangeBackupToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .used(false)
                .build();

        backupTokenRepository.save(backupToken);

        String backupLink = "http://localhost:" + serverPort + "/account/change-password/backup?token=" + token;
        emailService.sendPasswordChangedEmail(user.getEmail(), backupLink);
    }

    public enum BackupTokenStatus {
        VALID,
        INVALID,
        EXPIRED
    }
}
