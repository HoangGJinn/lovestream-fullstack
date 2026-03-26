package com.hcmute.lovestream.service.authentication;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import com.hcmute.lovestream.entity.enums.UserStatus;
import com.hcmute.lovestream.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom OIDC user service that implements find-or-create logic for Google login.
 * After Google authenticates, this service:
 * 1. Finds existing user by email (merges Google login with existing email/password account).
 * 2. Creates a new user (isActive=true, password=null) if email not found.
 * 3. Auto-verifies unverified accounts (isActive=false) since Google has confirmed the email.
 * 4. Rejects BANNED/REMOVED accounts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final UserRepository userRepository;
    private final OidcUserService delegate = new OidcUserService();

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Delegate to Spring's default OIDC user service to parse the token
        OidcUser oidcUser = delegate.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String picture = oidcUser.getPicture();

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_email"), "Không lấy được email từ tài khoản Google.");
        }

        // 2. Find-or-create user by email
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Case 1: Email chưa tồn tại → tạo mới
            log.info("Google OAuth2: Tạo tài khoản mới cho email={}", email);
            user = User.builder()
                    .fullName(name != null ? name : email)
                    .email(email)
                    .phone(null)
                    .password(null)          // Google users have no password
                    .avatar(picture)
                    .role(Role.USER)
                    .isActive(true)          // Google has already verified the email
                    .build();
            userRepository.save(user);

        } else {
            // Case 2 & 3: Email đã tồn tại
            if (user.getStatus() == UserStatus.BANNED) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_banned"), "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.");
            }
            if (user.getStatus() == UserStatus.REMOVED) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_removed"), "Tài khoản của bạn đã bị xóa.");
            }

            // Case 3: Email có nhưng chưa xác thực → auto-verify vì Google đã xác nhận
            if (!user.isActive()) {
                log.info("Google OAuth2: Tự động xác thực email cho user email={}", email);
                user.setActive(true);
                userRepository.save(user);
            }
            // Case 2: Email đã xác thực + có password → dùng lại, không thay đổi gì (merge)
        }

        return oidcUser;
    }
}
