package com.hcmute.lovestream.service.user;

import com.hcmute.lovestream.dto.request.UpdateProfileRequest;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getCurrentUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    @Transactional
    public void updateCurrentUserProfile(String email, UpdateProfileRequest request) {
        User user = getCurrentUserByEmail(email);

        String normalizedPhone = normalizeNullableValue(request.getPhone());
        if (normalizedPhone != null && userRepository.existsByPhoneAndIdNot(normalizedPhone, user.getId())) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại trong hệ thống");
        }

        user.setFullName(request.getFullName().trim());
        user.setPhone(normalizedPhone);
        user.setGender(request.getGender());
        user.setAvatar(normalizeNullableValue(request.getAvatar()));
        userRepository.save(user);
    }

    private String normalizeNullableValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
