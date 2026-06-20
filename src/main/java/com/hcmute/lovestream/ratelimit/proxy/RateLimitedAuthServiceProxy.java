package com.hcmute.lovestream.ratelimit.proxy;

import com.hcmute.lovestream.dto.request.*;
import com.hcmute.lovestream.ratelimit.RateLimitConfig;
import com.hcmute.lovestream.ratelimit.RateLimiter;
import com.hcmute.lovestream.service.authentication.AuthService;
import com.hcmute.lovestream.service.authentication.AuthServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Cơ chế hoạt động:
 * 1. Spring inject Proxy này thay cho AuthServiceImpl (@Primary)
 * 2. Controller gọi authService.login(...)
 * 3. Proxy nhận request → kiểm tra RateLimiter theo IP
 * 4a. Nếu PASS: delegate xuống AuthServiceImpl → trả kết quả về Controller
 * 4b. Nếu FAIL: ném RateLimitExceededException → GlobalExceptionHandler → HTTP
 * 429
 */
@Slf4j
@Service
@Primary // Spring sẽ inject Proxy này bất cứ khi nào cần AuthService
public class RateLimitedAuthServiceProxy implements AuthService {

    private final AuthServiceImpl realAuthService; // RealSubject
    private final RateLimiter rateLimiter; // Guard

    public RateLimitedAuthServiceProxy(
            AuthServiceImpl realAuthService,
            @Qualifier("redisRateLimiter") RateLimiter rateLimiter) {
        this.realAuthService = realAuthService;
        this.rateLimiter = rateLimiter;
    }

    // =============================================================
    // UC1 — Đăng ký: Rate limit theo IP
    // Giới hạn: 5 lần / 1 giờ
    // =============================================================
    public void register(Register request, String clientIp) {
        rateLimiter.checkLimit(
                RateLimitConfig.buildKey(RateLimitConfig.REGISTER_KEY, clientIp),
                RateLimitConfig.REGISTER_MAX,
                RateLimitConfig.REGISTER_WINDOW_SEC);
        log.debug("[RateLimit] PASS register ip={}", clientIp);
        realAuthService.register(request);
    }

    @Override
    public void register(Register request) {
        // Overload không có IP — delegate trực tiếp (dùng khi không cần rate limit)
        realAuthService.register(request);
    }

    // =============================================================
    // UC3 — Đăng nhập: Rate limit theo IP
    // Giới hạn: 10 lần / 15 phút
    // =============================================================
    public Map<String, String> login(Login request, String userAgent, String clientIp) {
        rateLimiter.checkLimit(
                RateLimitConfig.buildKey(RateLimitConfig.LOGIN_KEY, clientIp),
                RateLimitConfig.LOGIN_MAX,
                RateLimitConfig.LOGIN_WINDOW_SEC);
        log.debug("[RateLimit] PASS login ip={}", clientIp);
        return realAuthService.login(request, userAgent);
    }

    @Override
    public Map<String, String> login(Login request, String userAgent) {
        // Overload không có IP — delegate trực tiếp
        return realAuthService.login(request, userAgent);
    }

    // =============================================================
    // UC4 — Quên mật khẩu: Rate limit theo IP
    // Giới hạn: 3 lần / 10 phút
    // =============================================================
    public void forgotPassword(String email, String clientIp) {
        rateLimiter.checkLimit(
                RateLimitConfig.buildKey(RateLimitConfig.FORGOT_PASSWORD_KEY, clientIp),
                RateLimitConfig.FORGOT_PASSWORD_MAX,
                RateLimitConfig.FORGOT_PASSWORD_WINDOW_SEC);
        log.debug("[RateLimit] PASS forgotPassword ip={}", clientIp);
        realAuthService.forgotPassword(email);
    }

    @Override
    public void forgotPassword(String email) {
        realAuthService.forgotPassword(email);
    }

    // =============================================================
    // UC4 — Xác minh OTP (chống brute-force): Rate limit theo IP
    // Giới hạn: 5 lần / 5 phút
    // =============================================================
    public void verifyForgotPasswordOtp(String token, String clientIp) {
        rateLimiter.checkLimit(
                RateLimitConfig.buildKey(RateLimitConfig.VERIFY_OTP_KEY, clientIp),
                RateLimitConfig.VERIFY_OTP_MAX,
                RateLimitConfig.VERIFY_OTP_WINDOW_SEC);
        log.debug("[RateLimit] PASS verifyForgotPasswordOtp ip={}", clientIp);
        realAuthService.verifyForgotPasswordOtp(token);
    }

    @Override
    public void verifyForgotPasswordOtp(String token) {
        realAuthService.verifyForgotPasswordOtp(token);
    }

    // =============================================================
    // UC2 — Resend OTP: Rate limit theo IP
    // Giới hạn: 3 lần / 10 phút
    // =============================================================
    public void resendOtp(String email, String clientIp) {
        rateLimiter.checkLimit(
                RateLimitConfig.buildKey(RateLimitConfig.RESEND_OTP_KEY, clientIp),
                RateLimitConfig.RESEND_OTP_MAX,
                RateLimitConfig.RESEND_OTP_WINDOW_SEC);
        log.debug("[RateLimit] PASS resendOtp ip={}", clientIp);
        realAuthService.resendOtp(email);
    }

    @Override
    public void resendOtp(String email) {
        realAuthService.resendOtp(email);
    }

    // =============================================================
    // Pass-through methods (không rate limit)
    // Lý do: đã có cơ chế bảo vệ riêng (token, OTP đã xác minh, v.v.)
    // =============================================================

    @Override
    public void verifyEmail(String token) {
        realAuthService.verifyEmail(token);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        realAuthService.resetPassword(token, newPassword);
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        return realAuthService.refreshToken(refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        realAuthService.logout(refreshToken);
    }

    @Override
    public Map<String, String> googleLogin(String email, String deviceId, String userAgent) {
        return realAuthService.googleLogin(email, deviceId, userAgent);
    }
}
