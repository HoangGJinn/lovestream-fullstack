package com.hcmute.lovestream.ratelimit;

/**
 * Interface trung tâm của Proxy Pattern cho Rate Limiting.
 * - "Guard" : RateLimiter (interface này) — được Proxy dùng để ra quyết định
 * chặn/cho qua
 *
 * Có 2 implementation:
 * 1. RedisRateLimiter — dùng Redis (Valkey) làm storage (production)
 * 2. InMemoryRateLimiter — dùng ConcurrentHashMap (fallback khi Redis không khả
 * dụng)
 */
public interface RateLimiter {

    /**
     * Kiểm tra và ghi nhận một lượt request từ identifier (ví dụ: IP address).
     * Nếu vượt quá giới hạn trong cửa sổ thời gian, ném RateLimitExceededException.
     *
     * @param key           Khoá định danh duy nhất, ví dụ: "rl:login:192.168.1.1"
     * @param maxRequests   Số lượt tối đa được phép trong cửa sổ thời gian
     * @param windowSeconds Độ rộng cửa sổ thời gian (giây)
     * @throws RateLimitExceededException nếu đã vượt giới hạn
     */
    void checkLimit(String key, int maxRequests, int windowSeconds);
}
