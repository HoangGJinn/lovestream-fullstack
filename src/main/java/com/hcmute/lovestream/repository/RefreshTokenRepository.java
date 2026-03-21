package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.RefreshToken;
import com.hcmute.lovestream.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Tìm token để kiểm tra tính hợp lệ khi user gọi API lấy Access Token mới
    Optional<RefreshToken> findByToken(String token);

    // Lấy tất cả token của 1 user (hữu ích khi muốn làm tính năng "Đăng xuất khỏi tất cả thiết bị")
    List<RefreshToken> findAllByUser(User user);

    // Thu hồi (Revoke) tất cả token đang hợp lệ của một user cụ thể
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
    void revokeAllUserTokens(@Param("userId") String userId);

    // Dọn dẹp Database: Xóa các token đã hết hạn hoặc đã bị thu hồi (có thể dùng Cron Job để chạy mỗi ngày)
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR r.revoked = true")
    void deleteAllExpiredOrRevokedTokens(@Param("now") LocalDateTime now);
    void deleteByUser(User user);
}
