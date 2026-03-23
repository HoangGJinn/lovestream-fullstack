package com.hcmute.lovestream.service.admin.user;

import com.hcmute.lovestream.dto.request.admin.user.CreateContentManagerRequest;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import com.hcmute.lovestream.entity.enums.UserStatus;
import com.hcmute.lovestream.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminUserManagementServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserManagementServiceImpl adminUserManagementService;

    private CreateContentManagerRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = CreateContentManagerRequest.builder()
                .fullName("Test Manager")
                .email("test@lovestream.com")
                .phone("0123456789")
                .password("password")
                .confirmPassword("password")
                .build();
    }

    @Test
    void whenCreateContentManager_thenSuccess() {
        // Mock
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(validRequest.getPhone())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encoded-password");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId("mock-id");
            return savedUser;
        });

        // Execute
        User result = adminUserManagementService.createContentManager(validRequest);

        // Verify
        assertThat(result).isNotNull();
        assertThat(result.getFullName()).isEqualTo("Test Manager");
        assertThat(result.getEmail()).isEqualTo("test@lovestream.com");
        assertThat(result.getPhone()).isEqualTo("0123456789");
        assertThat(result.getPassword()).isEqualTo("encoded-password");

        // Verify hardcoded business rules
        assertThat(result.getRole()).isEqualTo(Role.CONTENT_MANAGER);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);

        // Verify interactions
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password");
    }

    @Test
    void whenEmailExists_thenThrowsException() {
        // Mock
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        // Execute & Verify
        assertThatThrownBy(() -> adminUserManagementService.createContentManager(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email đã tồn tại trong hệ thống");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenPhoneExists_thenThrowsException() {
        // Mock
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(validRequest.getPhone())).thenReturn(true);

        // Execute & Verify
        assertThatThrownBy(() -> adminUserManagementService.createContentManager(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Số điện thoại đã tồn tại trong hệ thống");

        verify(userRepository, never()).save(any(User.class));
    }
}
