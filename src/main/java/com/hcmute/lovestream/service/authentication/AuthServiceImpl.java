package com.hcmute.lovestream.service.authentication;

import com.hcmute.lovestream.dto.request.*;
import com.hcmute.lovestream.entity.RefreshToken;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import com.hcmute.lovestream.entity.enums.UserStatus;
import com.hcmute.lovestream.repository.RefreshTokenRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.security.JwtUtil;
import com.hcmute.lovestream.service.email.EmailService;
import com.hcmute.lovestream.service.otp.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService; // Dùng OtpService thay cho Repository
    // private final EmailService emailService;
    private final EmailService emailService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshExpiration;

    @Override
    @Transactional
    public void register(Register request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .isActive(false)
                .build();
        userRepository.save(user);

        // Tạo mã OTP lưu vào RAM
        String otp = otpService.generateAndSaveOtp(user.getEmail());
        emailService.sendVerificationEmail(user.getEmail(), otp);

        // TODO: Gửi mã OTP này qua email (Tạm thời in ra console để test)
        System.out.println("MÃ OTP XÁC NHẬN EMAIL CHO " + request.getEmail() + " LÀ: " + otp);
    }

    @Override
    @Transactional
    public void verifyEmail(String otp) {
        // Lấy email từ RAM dựa vào mã OTP
        String email = otpService.getEmailByOtp(otp);
        if (email == null) {
            throw new RuntimeException("Mã xác nhận không hợp lệ hoặc đã hết hạn");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setActive(true);
        userRepository.save(user);

        // Xóa OTP khỏi RAM
        otpService.clearOtp(otp);
    }

    @Override
    @Transactional
    public Map<String, String> login(Login request) { // Đổi kiểu trả về thành Map
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Tài khoản chưa được xác minh email");
        }

        if(user.getStatus() == UserStatus.BANNED) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }

        if(user.getStatus() == UserStatus.REMOVED) {
            throw new RuntimeException("Tài khoản của bạn đã bị xóa. Vui lòng sử dụng tài khoản khác.");
        }

// 1. Tạo Access Token (Vé xem phim - Hạn ngắn)
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        // 2. Tạo Refresh Token (Thẻ thành viên - Hạn dài)
        String refreshTokenString = java.util.UUID.randomUUID().toString();

        // Xóa các Refresh Token cũ của user này để tránh rác DB
        refreshTokenRepository.deleteByUser(user);

        // Tính toán thời gian hết hạn (Cộng thêm số Giây vào thời gian hiện tại)
        // refreshExpiration của bạn đang là mili-giây (604800000), nên chia 1000 để ra giây
        LocalDateTime expiresAtTime = LocalDateTime.now().plusSeconds(refreshExpiration / 1000);

        // Tận dụng @Builder cực xịn từ file Entity của bạn
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenString)
                .expiresAt(expiresAtTime)
                .revoked(false)
                // createdAt đã được @CreationTimestamp tự động lo
                .build();

        // Lưu xuống bảng refresh_tokens
        refreshTokenRepository.save(refreshToken);

        // 3. Đóng gói cả hai Token vào Map và trả về
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshTokenString);

        return tokens;
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy email trong hệ thống"));

        if (user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.REMOVED) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa hoặc đã bị xóa. Không thể thao tác.");
        }

        // Tạo và lưu OTP vào cache
        String otp = otpService.generateAndSaveOtp(email);

        emailService.sendResetPasswordEmail(email, otp);

        // TODO: Gửi OTP qua email (Tạm thời in ra console để test)
        System.out.println("MÃ OTP ĐẶT LẠI MẬT KHẨU CHO " + email + " LÀ: " + otp);
    }

    @Override
    @Transactional
    public void resetPassword(String otp, String newPassword) {
        String email = otpService.getEmailByOtp(otp);
        if (email == null) {
            throw new RuntimeException("Mã xác nhận không hợp lệ hoặc đã hết hạn");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.REMOVED) {
            throw new RuntimeException("Tài khoản đang bị khóa hoặc đã xóa. Không thể đặt lại mật khẩu.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Thu hồi tất cả Refresh Token cũ — đảm bảo các thiết bị khác bị đăng xuất sau khi đổi mật khẩu
        refreshTokenRepository.revokeAllUserTokens(user.getId());

        otpService.clearOtp(otp);
    }

    @Override
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.REMOVED) {
            throw new RuntimeException("Tài khoản đang bị khóa hoặc đã xóa.");
        }

        if (user.isActive()) {
            throw new RuntimeException("Tài khoản này đã được xác minh.");
        }

        // Tạo mã mới và gửi lại email chạy ngầm
        String otp = otpService.generateAndSaveOtp(email);
        emailService.sendVerificationEmail(email, otp);
    }

    @Override
    @Transactional
    public Map<String, String> refreshToken(String refreshTokenStr) {
        // 1. Tìm Refresh Token trong Database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Refresh Token không hợp lệ hoặc không tồn tại"));

        // 2. Kiểm tra cờ Revoked (Bị thu hồi do đăng xuất hoặc đổi mật khẩu)
        if (refreshToken.isRevoked()) {
            // Phát hiện Refresh Token Reuse Attack: đã bị revoke mà vẫn được dùng lại
            // => Thu hồi TOÀN BỘ token của user này để bảo vệ tài khoản
            refreshTokenRepository.revokeAllUserTokens(refreshToken.getUser().getId());
            throw new RuntimeException("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
        }

        // 3. Kiểm tra Hạn sử dụng
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new RuntimeException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }

        // 4. Mọi thứ hợp lệ -> Token Rotation: Thu hồi token cũ, cấp token mới
        User user = refreshToken.getUser();

        // 4a. Revoke token cũ
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // 4b. Tạo Access Token mới
        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        // 4c. Tạo Refresh Token mới và lưu vào DB
        String newRefreshTokenStr = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshExpiration / 1000);
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(newRefreshTokenStr)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        // 4d. Trả về cả hai token
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshTokenStr);
        return tokens;
    }

    @Override
    @Transactional
    public void logout(String refreshTokenStr) {
        // Tìm và revoke Refresh Token trong DB
        // Nếu không tìm thấy token thì cũng không cần throw lỗi (idempotent)
        refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }
}