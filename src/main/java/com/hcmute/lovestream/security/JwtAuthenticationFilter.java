package com.hcmute.lovestream.security;

import com.hcmute.lovestream.entity.RefreshToken;
import com.hcmute.lovestream.repository.RefreshTokenRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshExpiration;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Lấy token từ request (Cookie hoặc Header)
        String token = jwtUtil.extractTokenFromRequest(request);

        // 2. Nếu có token và chưa có thông tin xác thực trong SecurityContext
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtUtil.extractUsername(token);

                if (email != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    // 3. Kiểm tra tính hợp lệ của token VÀ đảm bảo đây là Access Token (không phải Refresh Token)
                    if (jwtUtil.validateToken(token, userDetails) && "ACCESS".equals(jwtUtil.extractTokenType(token))) {
                        // 4. Cấp quyền truy cập
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }

            } catch (ExpiredJwtException e) {
                // Access Token hết hạn → thử auto-refresh bằng REFRESH_TOKEN cookie
                logger.debug("Access Token hết hạn, đang thử auto-refresh...");
                tryAutoRefresh(request, response, e.getClaims().getSubject());

            } catch (JwtException e) {
                // Token bị giả mạo, sai chữ ký, hoặc malformed — bỏ qua
                logger.warn("JWT token không hợp lệ: " + e.getMessage());
            }
        }

        // Cho qua chốt kiểm tra
        filterChain.doFilter(request, response);
    }

    /**
     * Tự động làm mới Access Token khi hết hạn (dành cho Thymeleaf web app).
     * Nếu REFRESH_TOKEN cookie hợp lệ: cấp token mới vào cookie và authenticate request ngay.
     * Nếu không hợp lệ: bỏ qua → Spring Security sẽ redirect về /login.
     */
    private void tryAutoRefresh(HttpServletRequest request, HttpServletResponse response, String email) {
        // Lấy giá trị REFRESH_TOKEN từ cookie
        String refreshTokenValue = extractRefreshTokenCookie(request);
        if (refreshTokenValue == null) {
            logger.debug("Không tìm thấy REFRESH_TOKEN cookie, yêu cầu đăng nhập lại.");
            return;
        }

        // Tìm trong database
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresentOrElse(rt -> {
            // Kiểm tra chưa bị revoke và chưa hết hạn
            if (rt.isRevoked() || rt.getExpiresAt().isBefore(LocalDateTime.now())) {
                logger.debug("REFRESH_TOKEN không hợp lệ hoặc đã hết hạn, yêu cầu đăng nhập lại.");
                return;
            }

            // ---- Token Rotation ----
            // 1. Revoke refresh token cũ
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);

            // 2. Tạo refresh token mới
            String newRefreshTokenStr = UUID.randomUUID().toString();
            RefreshToken newRefreshToken = RefreshToken.builder()
                    .user(rt.getUser())
                    .token(newRefreshTokenStr)
                    .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                    .revoked(false)
                    .build();
            refreshTokenRepository.save(newRefreshToken);

            // 3. Tạo Access Token mới
            String newAccessToken = jwtUtil.generateToken(rt.getUser().getEmail(), rt.getUser().getRole());

            // 4. Ghi hai cookie mới vào response
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

            // 5. Authenticate request hiện tại (user không bị redirect)
            UserDetails userDetails = userDetailsService.loadUserByUsername(rt.getUser().getEmail());
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            logger.debug("Auto-refresh thành công cho user: " + rt.getUser().getEmail());

        }, () -> logger.debug("Không tìm thấy REFRESH_TOKEN trong database."));
    }

    /** Trích xuất giá trị của REFRESH_TOKEN cookie từ request */
    private String extractRefreshTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("REFRESH_TOKEN".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
