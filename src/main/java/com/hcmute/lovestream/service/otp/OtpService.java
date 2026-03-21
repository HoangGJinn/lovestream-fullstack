package com.hcmute.lovestream.service.otp;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    // Cache lưu trữ: Key là Mã OTP/Token, Value là Email của user
    private final Cache<String, String> otpCache;

    public OtpService() {
        this.otpCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES) // Mã tự động hết hạn và xóa khỏi RAM sau 5 phút
                .build();
    }

    // Tạo mã OTP ngẫu nhiên 6 ký tự và lưu vào Cache
    public String generateAndSaveOtp(String email) {
        String otp = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        otpCache.put(otp, email);
        return otp;
    }

    // Lấy email dựa vào mã OTP
    public String getEmailByOtp(String otp) {
        return otpCache.getIfPresent(otp);
    }

    // Xóa mã OTP khỏi RAM sau khi đã sử dụng thành công
    public void clearOtp(String otp) {
        otpCache.invalidate(otp);
    }
}
