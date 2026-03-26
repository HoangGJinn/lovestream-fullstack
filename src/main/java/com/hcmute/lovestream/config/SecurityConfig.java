package com.hcmute.lovestream.config;

import com.hcmute.lovestream.security.JwtAuthenticationFilter;
import com.hcmute.lovestream.security.OAuth2AuthenticationSuccessHandler;
import com.hcmute.lovestream.service.authentication.GoogleOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;



    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // STATELESS for API, but OAuth2 redirect flow needs a minimal session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // 1. Auth pages + Static resources + SEO files
                        .requestMatchers("/api/v1/auth/**", "/login", "/register", "/forgot-password", "/verify-email",
                                "/css/**", "/js/**", "/images/**", "/error", "/sitemap.xml", "/robots.txt").permitAll()
                        // OAuth2 endpoints must be public
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/account/change-password/backup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/password/backup-change").permitAll()

                        // 2. Trang công khai cho khách chưa đăng nhập (SEO)
                        .requestMatchers(HttpMethod.GET, "/", "/home").permitAll()
                        .requestMatchers(HttpMethod.GET, "/movies", "/movies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/series", "/series/**").permitAll()
                        .requestMatchers("/videocontents", "/videocontents/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/plans", "/plans/**", "/packages", "/packages/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/comments/**", "/api/v1/ratings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/api/vnpay/payment-callback").permitAll()

                        // 3. Admin entrypoint + content modules: ADMIN or CONTENT_MANAGER
                        .requestMatchers(
                                "/admin", "/admin/dashboard",
                                "/admin/movies", "/admin/movies/**",
                                "/admin/series", "/admin/series/**",
                                "/admin/genres", "/admin/genres/**"
                        ).hasAnyAuthority("ROLE_ADMIN", "ROLE_CONTENT_MANAGER", "ADMIN", "CONTENT_MANAGER")

                        // 4. Admin-restricted modules: ADMIN only
                        .requestMatchers("/admin/users/**", "/admin/plans/**", "/admin/vouchers/**")
                                .hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // 5. Phần còn lại của admin: ADMIN only (safety net)
                        .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // CÁC TRANG CÒN LẠI BẮT BUỘC PHẢI ĐĂNG NHẬP
                        .anyRequest().authenticated()
                )
                // Xử lý khi bị chặn (Chưa đăng nhập)
                .exceptionHandling(exc -> exc.authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập");
                    } else {
                        response.sendRedirect("/login");
                    }
                }))
                // Google OAuth2 Login configuration
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(googleOAuth2UserService)
                        )
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            // Log lỗi thực sự để dễ debug
                            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class)
                                    .error("Google OAuth2 login FAILED: {}", exception.getMessage(), exception);
                            // Xóa session tránh ghost auth
                            jakarta.servlet.http.HttpSession session = request.getSession(false);
                            if (session != null) session.invalidate();
                            // Redirect với message lỗi rõ ràng
                            String msg = exception.getMessage() != null ? exception.getMessage() : "Đăng nhập Google thất bại";
                            try {
                                response.sendRedirect("/login?error=" + java.net.URLEncoder.encode(msg, "UTF-8"));
                            } catch (Exception e) {
                                response.sendRedirect("/login?error=google_login_failed");
                            }
                        })
                )
                // Chèn chốt kiểm tra JWT vào trước chốt kiểm tra mặc định của Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}