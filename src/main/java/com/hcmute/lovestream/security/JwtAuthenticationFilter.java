package com.hcmute.lovestream.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Lấy token từ request (Cookie hoặc Header)
        String token = jwtUtil.extractTokenFromRequest(request);

        // 2. Nếu có token và chưa có thông tin xác thực trong SecurityContext
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtUtil.extractUsername(token);

            if (email != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 3. Kiểm tra tính hợp lệ của token
                if (jwtUtil.validateToken(token, userDetails)) {
                    // 4. Cấp quyền truy cập
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        // Cho qua chốt kiểm tra
        filterChain.doFilter(request, response);
    }
}