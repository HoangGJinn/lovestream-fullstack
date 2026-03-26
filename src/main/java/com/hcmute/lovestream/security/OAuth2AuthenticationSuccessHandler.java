package com.hcmute.lovestream.security;

import com.hcmute.lovestream.service.authentication.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Handles the redirect after a successful Google OAuth2 / OIDC login.
 * 1. Extracts email from principal (supports both OidcUser and DefaultOAuth2User)
 * 2. Calls authService.googleLogin() to issue JWT + RefreshToken
 * 3. Writes JWT_TOKEN and REFRESH_TOKEN as HttpOnly cookies
 * 4. INVALIDATES the OAuth2 session – prevents "session auth without JWT" confusion
 * 5. Redirects to the appropriate page based on role
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String email = extractEmail(authentication);

        if (email == null || email.isBlank()) {
            log.error("Google OAuth2: không lấy được email từ principal loại {}",
                    authentication.getPrincipal().getClass().getName());
            invalidateSession(request);
            response.sendRedirect("/login?error=Không+lấy+được+email+từ+tài+khoản+Google");
            return;
        }

        try {
            Map<String, String> tokens = authService.googleLogin(email);

            // Xóa OAuth2 session SAU KHI lấy được token – tránh xung đột session/JWT
            invalidateSession(request);

            // Set JWT_TOKEN cookie (Access Token)
            Cookie accessCookie = new Cookie("JWT_TOKEN", tokens.get("accessToken"));
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(86400);
            response.addCookie(accessCookie);

            // Set REFRESH_TOKEN cookie
            Cookie refreshCookie = new Cookie("REFRESH_TOKEN", tokens.get("refreshToken"));
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(604800);
            response.addCookie(refreshCookie);

            // Redirect theo role
            String role = tokens.get("role");
            String redirectUrl = "/home";

            if (role != null && !role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }
            if ("ROLE_ADMIN".equals(role)) {
                redirectUrl = "/admin/dashboard";
            } else if ("ROLE_CONTENT_MANAGER".equals(role)) {
                redirectUrl = "/admin/movies";
            }

            log.info("Google OAuth2 login success: email={}, redirect={}", email, redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Google OAuth2 post-login error for email={}: {}", email, e.getMessage(), e);
            invalidateSession(request);
            response.sendRedirect("/login?error=" + java.net.URLEncoder.encode(e.getMessage(), "UTF-8"));
        }
    }

    /** Xóa session OAuth2 để tránh session auth tồn tại song song với JWT cookie */
    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** Lấy email từ principal – hỗ trợ OidcUser và DefaultOAuth2User */
    private String extractEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            return email != null ? email.toString() : null;
        }
        return null;
    }
}
