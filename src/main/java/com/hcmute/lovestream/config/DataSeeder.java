package com.hcmute.lovestream.config;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import com.hcmute.lovestream.entity.enums.UserStatus;
import com.hcmute.lovestream.repository.MovieRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.service.MovieSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MovieRepository movieRepository;
    private final MovieSyncService movieSyncService;

    @Override
    public void run(String... args) {
        seedTestUsers();
        seedMoviesIfNeeded();
    }

    private void seedTestUsers() {
        if (!userRepository.existsByEmail("admin_test@lovestream.com")) {
            User admin = User.builder()
                    .email("admin_test@lovestream.com")
                    .fullName("Test Administrator")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .isActive(true)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
            log.info("Created test admin: admin_test@lovestream.com");
        }

        if (!userRepository.existsByEmail("content_test@lovestream.com")) {
            User manager = User.builder()
                    .email("content_test@lovestream.com")
                    .fullName("Test Content Manager")
                    .password(passwordEncoder.encode("content123"))
                    .role(Role.CONTENT_MANAGER)
                    .isActive(true)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(manager);
            log.info("Created test content manager: content_test@lovestream.com");
        }
    }

    private void seedMoviesIfNeeded() {
        if (movieRepository.count() == 0) {
            log.info("Movie database is empty. Fetching seed movies from free API...");
            movieSyncService.fetchAndSaveFreeMovies();
        }
    }
}
