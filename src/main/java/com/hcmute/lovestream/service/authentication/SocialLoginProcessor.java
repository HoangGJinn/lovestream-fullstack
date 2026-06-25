package com.hcmute.lovestream.service.authentication;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import com.hcmute.lovestream.entity.enums.UserStatus;
import com.hcmute.lovestream.repository.UserRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

public abstract class SocialLoginProcessor {

    protected final UserRepository userRepository;

    protected SocialLoginProcessor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // --- TEMPLATE METHOD (Final) ---
    public final User process(Map<String, Object> attributes) {
        String email = extractEmail(attributes);
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_email"), "Không lấy được email từ tài khoản mạng xã hội.");
        }

        String fullName = extractFullName(attributes);
        String avatar = extractAvatar(attributes);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = User.builder()
                    .fullName(fullName != null ? fullName : email)
                    .email(email)
                    .phone(null)
                    .password(null)
                    .avatar(avatar)
                    .role(Role.USER)
                    .isActive(true)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
        } else {
            if (user.getStatus() == UserStatus.BANNED) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_banned"), "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.");
            }
            if (user.getStatus() == UserStatus.REMOVED) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_removed"), "Tài khoản của bạn đã bị xóa.");
            }

            if (!user.isActive()) {
                user.setActive(true);
                userRepository.save(user);
            }
        }

        return user;
    }

    // --- Primitive Operations ---
    protected abstract String extractEmail(Map<String, Object> attributes);
    protected abstract String extractFullName(Map<String, Object> attributes);
    protected abstract String extractAvatar(Map<String, Object> attributes);
}
