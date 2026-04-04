package com.hcmute.lovestream.security;

import com.hcmute.lovestream.entity.RefreshToken;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import com.hcmute.lovestream.repository.RefreshTokenRepository;
import com.hcmute.lovestream.repository.SubscriptionRepository;
import com.hcmute.lovestream.service.device.DeviceAccessService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DeviceAccessService deviceAccessService;

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshExpiration;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Bỏ qua hoàn toàn việc xác thực JWT cho các file tĩnh
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                || path.startsWith("/assets/") || path.startsWith("/webjars/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Lấy token từ request
        String token = jwtUtil.extractTokenFromRequest(request);

        // 2. Nếu có token và chưa xác thực
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (jwtUtil.isTokenValid(token) && "ACCESS".equals(jwtUtil.extractTokenType(token))) {

                    String email = jwtUtil.extractUsername(token);
                    String userId = jwtUtil.extractUserIdString(token);
                    String roleStr = jwtUtil.extractRole(token);
                    boolean isVip = jwtUtil.extractIsVip(token);
                    String fullName = jwtUtil.extractFullName(token);
                    String avatar = jwtUtil.extractAvatar(token);
                    String tokenDeviceId = jwtUtil.extractDeviceId(token);

                    boolean deviceActive = true;
                    if (tokenDeviceId != null && !tokenDeviceId.isBlank()) {
                        deviceActive = userId != null && !userId.isBlank()
                                ? deviceAccessService.isDeviceActiveByUserId(userId, tokenDeviceId)
                                : deviceAccessService.isDeviceActive(email, tokenDeviceId);
                    }
                    if (!deviceActive) {
                        clearAuthCookies(response);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (roleStr != null && !roleStr.startsWith("ROLE_")) {
                        roleStr = "ROLE_" + roleStr;
                    }

                    List<SimpleGrantedAuthority> authorities =
                            roleStr != null ? Collections.singletonList(new SimpleGrantedAuthority(roleStr)) : Collections.emptyList();

                    // Gom tất cả dữ liệu vào một Map (Principal)
                    JwtPrincipal principalData = new JwtPrincipal();
                    principalData.put("email", email);
                    principalData.put("userId", userId);
                    principalData.put("fullName", fullName);
                    principalData.put("avatar", avatar);
                    principalData.put("isVip", isVip);
                    principalData.put("deviceId", tokenDeviceId);

                    // Cấp quyền trực tiếp vào Context, truyền principalData làm tham số đầu tiên
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principalData, null, authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

            } catch (ExpiredJwtException e) {
                logger.debug("Access Token hết hạn, đang thử auto-refresh...");
                tryAutoRefresh(request, response, e.getClaims().getSubject());
            } catch (JwtException e) {
                logger.warn("JWT token không hợp lệ: " + e.getMessage());
                clearAuthCookies(response);
            } catch (Exception e) {
                logger.warn("Lỗi xác thực JWT không mong muốn: " + e.getMessage());
                clearAuthCookies(response);
            }
        }

        // Cho qua chốt kiểm tra
        filterChain.doFilter(request, response);
    }

    private void tryAutoRefresh(HttpServletRequest request, HttpServletResponse response, String email) {
        String refreshTokenValue = extractRefreshTokenCookie(request);
        if (refreshTokenValue == null) {
            logger.debug("Không tìm thấy REFRESH_TOKEN cookie, yêu cầu đăng nhập lại.");
            clearAuthCookies(response);
            return;
        }

        // Tìm trong database
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresentOrElse(rt -> {

            if (email != null && !email.equalsIgnoreCase(rt.getUser().getEmail())) {
                logger.warn("REFRESH_TOKEN không khớp với subject trong access token.");
                clearAuthCookies(response);
                return;
            }

            if (rt.isRevoked() || rt.getExpiresAt().isBefore(LocalDateTime.now())) {
                logger.debug("REFRESH_TOKEN không hợp lệ hoặc đã hết hạn, yêu cầu đăng nhập lại.");
                clearAuthCookies(response);
                return;
            }

            if (rt.getDeviceId() != null && !rt.getDeviceId().isBlank()
                    && !deviceAccessService.isDeviceActiveByUserId(rt.getUser().getId(), rt.getDeviceId())) {
                clearAuthCookies(response);
                return;
            }

            // ---- Token Rotation ----
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);

            String newRefreshTokenStr = UUID.randomUUID().toString();
            RefreshToken newRefreshToken = RefreshToken.builder()
                    .user(rt.getUser())
                    .token(newRefreshTokenStr)
                    .deviceId(rt.getDeviceId())
                    .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                    .revoked(false)
                    .build();
            refreshTokenRepository.save(newRefreshToken);

            boolean isVip = subscriptionRepository.existsByUser_IdAndStatusAndEndDateAfter(
                    rt.getUser().getId(),
                    SubscriptionStatus.ACTIVE,
                    LocalDateTime.now()
            );

            // SỬA: Dùng hàm generateToken theo thiết kế mới (User user, boolean isVip)
            String newAccessToken = jwtUtil.generateToken(rt.getUser(), isVip, rt.getDeviceId());

            Cookie jwtCookie = new Cookie("JWT_TOKEN", newAccessToken);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(86400);
            response.addCookie(jwtCookie);

            Cookie refreshCookie = new Cookie("REFRESH_TOKEN", newRefreshTokenStr);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(604800);
            response.addCookie(refreshCookie);

            // Cấp quyền ngay cho request hiện tại để khỏi bị redirect (Không gọi userDetailsService nữa)
            JwtPrincipal principalData = new JwtPrincipal();
            principalData.put("email", rt.getUser().getEmail());
            principalData.put("userId", rt.getUser().getId());
            principalData.put("fullName", rt.getUser().getFullName());
            principalData.put("avatar", rt.getUser().getAvatar());
            principalData.put("isVip", isVip);
            principalData.put("deviceId", rt.getDeviceId());

            String roleStr = rt.getUser().getRole().getAuthority();
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleStr));

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    principalData, null, authorities
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            logger.debug("Auto-refresh thành công cho user: " + rt.getUser().getEmail());

        }, () -> {
            logger.debug("Không tìm thấy REFRESH_TOKEN trong database.");
            clearAuthCookies(response);
        });
    }

    private String extractRefreshTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("REFRESH_TOKEN".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("JWT_TOKEN", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        Cookie refreshCookie = new Cookie("REFRESH_TOKEN", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

        Cookie deviceCookie = new Cookie("DEVICE_ID", null);
        deviceCookie.setHttpOnly(false);
        deviceCookie.setPath("/");
        deviceCookie.setMaxAge(0);
        response.addCookie(deviceCookie);
    }

    public static class JwtPrincipal extends HashMap<String, Object> implements java.security.Principal {
        @Override
        public String getName() {
            return (String) get("email");
        }
    }
}
