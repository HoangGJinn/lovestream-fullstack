package com.hcmute.lovestream.config;

import com.hcmute.lovestream.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter; // Tiêm người gác cổng vào

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Cho phép các trang đăng nhập/đăng ký/css/hình ảnh không cần đăng nhập
                    .requestMatchers("/api/v1/auth/**", "/login", "/register", "/forgot-password", "/verify-email", "/css/**", "/js/**", "/images/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/account/change-password/backup").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/password/backup-change").permitAll()

                        // CÁC TRANG CÒN LẠI BẮT BUỘC PHẢI ĐĂNG NHẬP
                        .anyRequest().authenticated()
                )
                // Xử lý khi bị chặn (Chưa đăng nhập)
                .exceptionHandling(exc -> exc.authenticationEntryPoint((request, response, authException) -> {
                    // Nếu gọi API -> Báo lỗi 401
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập");
                    } else {
                        // Nếu là người dùng vào trang Web -> Đá về trang Đăng nhập
                        response.sendRedirect("/login");
                    }
                }))
                // Chèn chốt kiểm tra JWT vào trước chốt kiểm tra mặc định của Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}