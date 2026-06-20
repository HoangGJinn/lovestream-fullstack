package com.hcmute.lovestream.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Triển khai RateLimiter sử dụng bộ nhớ trong (ConcurrentHashMap) — Dùng làm fallback.
 *
 * Được kích hoạt khi Redis không khả dụng (Redis down, network error, v.v.).
 * Không phân tán — chỉ hoạt động chính xác trên môi trường single-instance.
 *
 * Lưu ý: InMemoryRateLimiter sẽ reset về 0 khi server restart.
 * Đây là giải pháp an toàn để ứng dụng không bị crash khi Redis tạm thời mất kết nối.
 */
@Component("inMemoryRateLimiter")
public class InMemoryRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRateLimiter.class);

    /**
     * Mỗi entry lưu: số đếm hiện tại + thời điểm cửa sổ kết thúc (epoch millis).
     */
    private static class WindowEntry {
        final AtomicInteger count = new AtomicInteger(0);
        final long windowEndMs;

        WindowEntry(long windowEndMs) {
            this.windowEndMs = windowEndMs;
        }
    }

    private final ConcurrentHashMap<String, WindowEntry> store = new ConcurrentHashMap<>();

    @Override
    public void checkLimit(String key, int maxRequests, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;

        // Lấy entry hiện tại; nếu không có hoặc đã hết hạn → tạo mới
        store.compute(key, (k, existing) -> {
            if (existing == null || now >= existing.windowEndMs) {
                return new WindowEntry(now + windowMs);
            }
            return existing;
        });

        WindowEntry entry = store.get(key);
        int currentCount = entry.count.incrementAndGet();

        if (currentCount > maxRequests) {
            long retryAfter = Math.max(1, (entry.windowEndMs - now) / 1000);
            log.warn("[RateLimit][InMemory] BLOCKED key={} count={}/{} retryAfter={}s",
                    key, currentCount, maxRequests, retryAfter);

            throw new RateLimitExceededException(
                    String.format("Bạn đã thực hiện quá nhiều yêu cầu. Vui lòng thử lại sau %d giây.", retryAfter),
                    retryAfter
            );
        }
    }
}
