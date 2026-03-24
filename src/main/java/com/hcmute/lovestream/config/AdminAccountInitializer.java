package com.hcmute.lovestream.config;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import com.hcmute.lovestream.entity.enums.UserStatus;
import com.hcmute.lovestream.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Kiểm tra chính xác email admin@admin.com
        if (!userRepository.existsByEmail("admin@admin.com")) {
            User adminUser = new User();

            adminUser.setFullName("Super Admin");
            // 2. Gán chính xác email admin@admin.com
            adminUser.setEmail("admin@admin.com");
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setRole(Role.ADMIN);
            adminUser.setActive(true);
            adminUser.setStatus(UserStatus.ACTIVE);

            userRepository.save(adminUser);
            // 3. In ra thông báo cho đúng
            System.out.println(" Đã khởi tạo tài khoản Admin mặc định (Email: admin@admin.com - Pass: admin)");
        }
    }
}