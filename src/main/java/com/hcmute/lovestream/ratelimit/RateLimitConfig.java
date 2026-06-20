package com.hcmute.lovestream.ratelimit;

/**
 * Cấu hình giới hạn Rate Limit cho từng use case.
 *
 * Mỗi use case được định nghĩa bằng một hằng số tĩnh (prefix + maxRequests + windowSeconds).
 * Proxy sẽ đọc các hằng số này để quyết định ngưỡng chặn.
 *
 * Định dạng key Redis: "rl:{KEY_PREFIX}:{identifier}"
 *   Ví dụ: "rl:register:203.0.113.5"
 */
public final class RateLimitConfig {

    private RateLimitConfig() {}

    // =========================================================
    // UC1 — Đăng ký tài khoản
    // Mục đích: Ngăn bot tạo tài khoản hàng loạt
    // =========================================================
    public static final String REGISTER_KEY         = "register";
    public static final int    REGISTER_MAX          = 5;
    public static final int    REGISTER_WINDOW_SEC   = 3600; // 1 giờ

    // =========================================================
    // UC3 — Đăng nhập
    // Mục đích: Ngăn brute-force mật khẩu
    // =========================================================
    public static final String LOGIN_KEY             = "login";
    public static final int    LOGIN_MAX             = 10;
    public static final int    LOGIN_WINDOW_SEC      = 900; // 15 phút

    // =========================================================
    // UC4 — Quên mật khẩu (gửi email reset)
    // Mục đích: Ngăn spam email hàng loạt
    // =========================================================
    public static final String FORGOT_PASSWORD_KEY   = "forgot_password";
    public static final int    FORGOT_PASSWORD_MAX   = 3;
    public static final int    FORGOT_PASSWORD_WINDOW_SEC = 600; // 10 phút

    // =========================================================
    // UC4 — Xác minh OTP quên mật khẩu
    // Mục đích: Ngăn brute-force mã OTP 6 số
    // =========================================================
    public static final String VERIFY_OTP_KEY        = "verify_otp";
    public static final int    VERIFY_OTP_MAX        = 5;
    public static final int    VERIFY_OTP_WINDOW_SEC = 300; // 5 phút

    // =========================================================
    // UC2 — Gửi lại OTP xác nhận email (Resend OTP)
    // Mục đích: Ngăn spam email xác nhận
    // =========================================================
    public static final String RESEND_OTP_KEY        = "resend_otp";
    public static final int    RESEND_OTP_MAX        = 3;
    public static final int    RESEND_OTP_WINDOW_SEC = 600; // 10 phút

    // =========================================================
    // Tiện ích: Tạo đầy đủ key Redis
    // Ví dụ: buildKey("login", "192.168.1.1") → "rl:login:192.168.1.1"
    // =========================================================
    public static String buildKey(String useCase, String identifier) {
        return "rl:" + useCase + ":" + identifier;
    }
}
