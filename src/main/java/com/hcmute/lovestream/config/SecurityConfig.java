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
                        // 1. Auth pages + Static resources + SEO files
                        .requestMatchers("/api/v1/auth/**", "/login", "/register", "/forgot-password", "/verify-email", "/css/**", "/js/**", "/images/**", "/error", "/sitemap.xml", "/robots.txt").permitAll()
                        .requestMatchers(HttpMethod.GET, "/account/change-password/backup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/password/backup-change").permitAll()

                        // 2. Trang công khai cho khách chưa đăng nhập (SEO)
                        // Trang chủ
                        .requestMatchers(HttpMethod.GET, "/", "/home").permitAll()
                        // Trang danh sách phim, chi tiết phim
                        .requestMatchers(HttpMethod.GET, "/movies", "/movies/**").permitAll()
                        // Trang series
                        .requestMatchers(HttpMethod.GET, "/series", "/series/**").permitAll()
                        // Tìm kiếm và lọc nội dung
                        .requestMatchers("/videocontents", "/videocontents/**").permitAll()
                        // API xem thông tin phim + trang xem phim
                        .requestMatchers(HttpMethod.GET, "/api/movies/**", "/watch-movie").permitAll()
                        // Trang gói dịch vụ
                        .requestMatchers(HttpMethod.GET, "/plans", "/plans/**", "/packages", "/packages/**").permitAll()
                        // Xem bình luận và đánh giá (chỉ GET, không cần đăng nhập)
                        .requestMatchers(HttpMethod.GET, "/api/v1/comments/**", "/api/v1/ratings/**").permitAll()
                        // VNPay callback
                        .requestMatchers(HttpMethod.GET, "/v1/api/vnpay/payment-callback").permitAll()

                        // 3. Admin entrypoint + content modules: ADMIN or CONTENT_MANAGER
                        .requestMatchers(
                                "/admin",
                                "/admin/dashboard",
                                "/admin/movies",
                                "/admin/movies/**",
                                "/admin/series",
                                "/admin/series/**",
                                "/admin/genres",
                                "/admin/genres/**"
                        ).hasAnyAuthority("ROLE_ADMIN", "ROLE_CONTENT_MANAGER", "ADMIN", "CONTENT_MANAGER")

                        // 4. Admin-restricted modules: ADMIN only
                        .requestMatchers("/admin/users/**", "/admin/plans/**", "/admin/vouchers/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // 5. Phần còn lại của admin: ADMIN only (safety net)
                        .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // CÁC TRANG CÒN LẠI BẮT BUỘC PHẢI ĐĂNG NHẬP
                        .anyRequest().authenticated()
                )
                // Xử lý khi bị chặn (Chưa đăng nhập)
                .exceptionHandling(exc -> exc.authenticationEntryPoint((request, response, authException) -> {
                    //Nếu gọi API -> Báo lỗi 401
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