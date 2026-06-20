package com.hcmute.lovestream.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Triển khai RateLimiter sử dụng Redis (Valkey) — Dùng cho môi trường production.
 *
 * Thuật toán: Fixed Window Counter với Redis INCR + EXPIRE.
 *   1. Mỗi request: INCR key → lấy số đếm hiện tại
 *   2. Nếu đây là lần đầu (count == 1): EXPIRE key = windowSeconds
 *   3. Nếu count > maxRequests: ném RateLimitExceededException
 *
 * Key format: "rl:{usecase}:{identifier}"
 *   Ví dụ: "rl:login:192.168.1.100"
 *
 * Bean được đánh dấu @Primary để Spring ưu tiên dùng Redis thay vì InMemoryRateLimiter.
 * Nếu Redis không khả dụng (exception), tự động fallback sang InMemoryRateLimiter.
 */
@Component("redisRateLimiter")
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final InMemoryRateLimiter fallback;

    public RedisRateLimiter(RedisTemplate<String, String> redisTemplate,
                            InMemoryRateLimiter fallback) {
        this.redisTemplate = redisTemplate;
        this.fallback = fallback;
    }

    @Override
    public void checkLimit(String key, int maxRequests, int windowSeconds) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);

            if (count == null) {
                // Redis trả về null — không thể xác định count, fallback
                log.warn("[RateLimit] Redis trả về null cho key={}, chuyển sang fallback", key);
                fallback.checkLimit(key, maxRequests, windowSeconds);
                return;
            }

            // Lần đầu tiên key được tạo → đặt TTL
            if (count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }

            if (count > maxRequests) {
                // Tính số giây còn lại của cửa sổ hiện tại để trả về Retry-After
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                long retryAfter = (ttl != null && ttl > 0) ? ttl : windowSeconds;

                log.warn("[RateLimit] BLOCKED key={} count={}/{} retryAfter={}s",
                        key, count, maxRequests, retryAfter);

                throw new RateLimitExceededException(
                        String.format("Bạn đã thực hiện quá nhiều yêu cầu. Vui lòng thử lại sau %d giây.", retryAfter),
                        retryAfter
                );
            }

        } catch (RateLimitExceededException e) {
            // Re-throw — không bắt exception nghiệp vụ
            throw e;
        } catch (Exception e) {
            // Redis không khả dụng → fallback sang in-memory
            log.warn("[RateLimit] Redis không khả dụng ({}), chuyển sang InMemoryRateLimiter", e.getMessage());
            fallback.checkLimit(key, maxRequests, windowSeconds);
        }
    }
}
