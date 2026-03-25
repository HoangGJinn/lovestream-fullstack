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

// THÊM 3 DÒNG IMPORT NÀY
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
                "role", u.getRole() != null ? u.getRole().name() : "USER", // Thêm check null an toàn
                "plan", u.getRole() == Role.USER ? "Chưa đăng ký" : "Quản trị",
                "status", u.getStatus() != null ? u.getStatus().name() : "INACTIVE" // Thêm check null an toàn
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

                // 3. Gán cứng thông số quan trọng (Role cố định, Không cần OTP xác thực)
                .role(Role.CONTENT_MANAGER)
                .isActive(true)
                .status(UserStatus.ACTIVE)
                .build();

        // 4. Lưu vào cơ sở dữ liệu
        return userRepository.save(manager);
    }
}