package com.hcmute.lovestream.service.admin.user;

import com.hcmute.lovestream.dto.request.admin.user.CreateContentManagerRequest;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import com.hcmute.lovestream.entity.enums.UserStatus;
import com.hcmute.lovestream.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserManagementServiceImpl implements AdminUserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<Map<String, Object>> getAllUsers(String searchKeyword) {
        List<User> users;

        // 1. Lấy dữ liệu từ DB (Có lọc theo email nếu người dùng gõ tìm kiếm)
        if (searchKeyword != null && !searchKeyword.isBlank()) {
            users = userRepository.findByEmailContainingIgnoreCase(searchKeyword.trim());
        } else {
            users = userRepository.findAll(); // Lấy tất cả nếu không tìm kiếm
        }

        // 2. Map dữ liệu sang JSON (chỉ lấy những trường giao diện cần)
        return users.stream().map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "email", u.getEmail(),
                "role", u.getRole() != null ? u.getRole().name() : "USER",
                "plan", (u.getRole() != null && u.getRole() == Role.USER) ? "Chưa đăng ký" : "Quản trị",

                // ĐÃ SỬA: Đảm bảo trả về đúng Enum BANNED để Frontend nhận diện
                "status", u.getStatus() != null ? u.getStatus().name() : "BANNED"
        )).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public User createContentManager(CreateContentManagerRequest request) {

        // 1. Kiểm tra tính duy nhất
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Số điện thoại đã tồn tại trong hệ thống");
        }

        // 2. Khởi tạo User Entity từ thông tin DTO
        User manager = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CONTENT_MANAGER)
                .isActive(false)
                .status(UserStatus.ACTIVE)
                .build();

        // 4. Lưu vào cơ sở dữ liệu
        return userRepository.save(manager);
    }

    @Override
    @Transactional
    public void toggleUserLock(String id, String reason) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Kiểm tra xem User có ĐANG BỊ KHÓA hay không (dựa vào Enum status)
        boolean isCurrentlyLocked = (user.getStatus() != UserStatus.ACTIVE);

        if (isCurrentlyLocked) {
            // NẾU ĐANG BANNED -> MỞ KHÓA VỀ ACTIVE
            user.setStatus(UserStatus.ACTIVE);
            user.setLockReason(null); // Xóa lý do
        } else {
            // NẾU ĐANG HOẠT ĐỘNG -> CHUYỂN THÀNH BANNED
            user.setStatus(UserStatus.BANNED);
            user.setLockReason(reason); // Lưu lý do Admin nhập
        }

        userRepository.save(user);
    }
}