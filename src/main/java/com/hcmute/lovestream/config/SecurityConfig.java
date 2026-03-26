package com.hcmute.lovestream.config;

import com.hcmute.lovestream.security.HttpCookieOAuth2AuthorizationRequestRepository;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    // Lưu trạng thái OAuth2 vào Cookie thay vì Session → tiết kiệm RAM
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // STATELESS hoàn toàn – OAuth2 state nằm ở Cookie phía trình duyệt
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/login",
                                "/register",
                                "/forgot-password",
                                "/verify-email",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/error",
                                "/sitemap.xml",
                                "/robots.txt"
                        ).permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/account/change-password/backup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/password/backup-change").permitAll()

                        // 2. Trang công khai cho khách chưa đăng nhập (SEO)
                        .requestMatchers(HttpMethod.GET, "/", "/home").permitAll()
                        // Trang tĩnh cho public
                        .requestMatchers(HttpMethod.GET, "/about", "/privacy-policy", "/terms").permitAll()
                        // Trang danh sách phim, chi tiết phim
                        .requestMatchers(HttpMethod.GET, "/movies", "/movies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/series", "/series/**").permitAll()
                        .requestMatchers("/videocontents", "/videocontents/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/plans", "/plans/**", "/packages", "/packages/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/comments/**", "/api/v1/ratings/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/api/vnpay/payment-callback").permitAll()


                        // 3. Admin entrypoint: ADMIN or CONTENT_MANAGER
                        .requestMatchers("/admin", "/admin/dashboard")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_CONTENT_MANAGER", "ADMIN", "CONTENT_MANAGER")

                        // 4. Content management modules: CONTENT_MANAGER only
                        .requestMatchers(
                                "/admin/movies",
                                "/admin/movies/**",
                                "/admin/series",
                                "/admin/series/**",
                                "/admin/genres",
                                "/admin/genres/**",
                                "/admin/web-content",
                                "/admin/web-content/**"
                        ).hasAnyAuthority("ROLE_CONTENT_MANAGER", "CONTENT_MANAGER")
                        .requestMatchers(
                                "/admin/users/**",
                                "/admin/plans/**",
                                "/admin/vouchers/**",
                                "/admin/transactions/**"
                        ).hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exc -> exc.authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Vui long dang nhap");
                    } else {
                        response.sendRedirect("/login");
                    }
                }))
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository)
                        )
                        .redirectionEndpoint(endpoint -> endpoint
                                .baseUri("/login/oauth2/code/*")
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(googleOAuth2UserService)
                        )
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class)
                                    .error("Google OAuth2 login FAILED: {}", exception.getMessage(), exception);

                            cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

                            String message = exception.getMessage() != null
                                    ? exception.getMessage()
                                    : "Dang nhap Google that bai";
                            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
                            response.sendRedirect("/login?error=" + encodedMessage);
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
