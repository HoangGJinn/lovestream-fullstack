package com.hcmute.lovestream.service.stream;

import com.hcmute.lovestream.dto.response.StreamSessionResponse;
import com.hcmute.lovestream.service.plan.ServicePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StreamSessionService {

    private static final String STREAM_KEY_PREFIX = "active_streams:";
    private static final String DEFAULT_LIMIT_REACHED_MESSAGE =
            "Tài khoản của bạn đã vượt quá số thiết bị xem cùng lúc, vui lòng đăng xuất.";

    private static final String TOUCH_STREAM_LUA = """
            local key = KEYS[1]
            local deviceId = ARGV[1]
            local nowMs = tonumber(ARGV[2])
            local expireAtMs = tonumber(ARGV[3])
            local maxDevices = tonumber(ARGV[4])
            local keyTtlMs = tonumber(ARGV[5])

            redis.call('ZREMRANGEBYSCORE', key, '-inf', nowMs)

            local existing = redis.call('ZSCORE', key, deviceId)
            if existing then
                redis.call('ZADD', key, expireAtMs, deviceId)
                redis.call('PEXPIRE', key, keyTtlMs)
                local activeCount = redis.call('ZCARD', key)
                return {1, activeCount}
            end

            local activeCount = redis.call('ZCARD', key)
            if activeCount >= maxDevices then
                return {0, activeCount}
            end

            redis.call('ZADD', key, expireAtMs, deviceId)
            redis.call('PEXPIRE', key, keyTtlMs)
            return {1, activeCount + 1}
            """;

    private static final String STOP_STREAM_LUA = """
            local key = KEYS[1]
            local deviceId = ARGV[1]
            local nowMs = tonumber(ARGV[2])

            redis.call('ZREMRANGEBYSCORE', key, '-inf', nowMs)
            redis.call('ZREM', key, deviceId)

            local activeCount = redis.call('ZCARD', key)
            if activeCount == 0 then
                redis.call('DEL', key)
            end
            return activeCount
            """;

    private final StringRedisTemplate redisTemplate;
    private final ServicePlanService servicePlanService;

    @Value("${app.stream-session.ttl-seconds:40}")
    private long streamTtlSeconds;

    @Value("${app.stream-session.key-ttl-buffer-seconds:5}")
    private long keyTtlBufferSeconds;

    public StreamSessionResponse start(String userEmail, String rawDeviceId) {
        return touch(userEmail, rawDeviceId, true);
    }

    public StreamSessionResponse heartbeat(String userEmail, String rawDeviceId) {
        return touch(userEmail, rawDeviceId, false);
    }

    public StreamSessionResponse stop(String userEmail, String rawDeviceId) {
        String key = streamKey(userEmail);
        String deviceId = normalizeDeviceId(rawDeviceId);
        long nowMs = Instant.now().toEpochMilli();

        RedisScript<Long> script = new DefaultRedisScript<>(STOP_STREAM_LUA, Long.class);
        try {
            Long activeCount = redisTemplate.execute(script, List.of(key), deviceId, String.valueOf(nowMs));
            int safeCount = activeCount == null ? 0 : Math.toIntExact(activeCount);
            int maxDevices = resolveMaxDevices(userEmail);
            return new StreamSessionResponse(true, safeCount, maxDevices, "Đã dừng phiên xem.");
        } catch (DataAccessException ex) {
            throw new IllegalStateException("Redis tạm thời không khả dụng.", ex);
        }
    }

    public Set<String> getActiveDeviceIds(String userEmail) {
        String key = streamKey(userEmail);
        long nowMs = Instant.now().toEpochMilli();

        try {
            redisTemplate.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, nowMs);
            Set<String> values = redisTemplate.opsForZSet().range(key, 0, -1);
            if (values == null || values.isEmpty()) {
                return Collections.emptySet();
            }
            return new HashSet<>(values);
        } catch (DataAccessException ex) {
            return Collections.emptySet();
        }
    }

    private StreamSessionResponse touch(String userEmail, String rawDeviceId, boolean isStartAction) {
        String key = streamKey(userEmail);
        String deviceId = normalizeDeviceId(rawDeviceId);
        int maxDevices = resolveMaxDevices(userEmail);

        long nowMs = Instant.now().toEpochMilli();
        long ttlMs = Math.max(streamTtlSeconds, 5L) * 1000L;
        long keyTtlMs = ttlMs + Math.max(keyTtlBufferSeconds, 1L) * 1000L;
        long expireAtMs = nowMs + ttlMs;

        RedisScript<List> script = new DefaultRedisScript<>(TOUCH_STREAM_LUA, List.class);
        List<?> result;
        try {
            result = redisTemplate.execute(
                    script,
                    List.of(key),
                    deviceId,
                    String.valueOf(nowMs),
                    String.valueOf(expireAtMs),
                    String.valueOf(maxDevices),
                    String.valueOf(keyTtlMs)
            );
        } catch (DataAccessException ex) {
            throw new IllegalStateException("Redis tạm thời không khả dụng.", ex);
        }

        if (result == null || result.size() < 2) {
            throw new IllegalStateException("Không nhận được phản hồi hợp lệ từ Redis.");
        }

        long allowedRaw = toLong(result.get(0));
        int activeDevices = Math.toIntExact(toLong(result.get(1)));
        boolean allowed = allowedRaw == 1L;

        if (!allowed) {
            return new StreamSessionResponse(false, activeDevices, maxDevices, DEFAULT_LIMIT_REACHED_MESSAGE + " (" + activeDevices + "/" + maxDevices + ")");
        }

        String message = isStartAction
                ? "Đã bắt đầu phiên xem."
                : "Heartbeat thành công.";
        return new StreamSessionResponse(true, activeDevices, maxDevices, message);
    }

    private String normalizeDeviceId(String rawDeviceId) {
        String normalized = rawDeviceId == null ? "" : rawDeviceId.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("deviceId không hợp lệ");
        }
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("deviceId vượt quá mức cho phép");
        }
        return normalized;
    }

    private String streamKey(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("Thong tin dang nhap khong hop le");
        }
        return STREAM_KEY_PREFIX + userEmail.trim().toLowerCase(Locale.ROOT);
    }

    private int resolveMaxDevices(String userEmail) {
        int maxDevices = servicePlanService.getMaxConcurrentStreams(userEmail);
        return Math.max(maxDevices, 1);
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(Objects.toString(value, "0"));
    }
}
