package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User admin;
    private User contentManager;
    private User normalUser;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .email("admin@test.com")
                .fullName("Admin")
                .password("encodedpass")
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);

        contentManager = User.builder()
                .email("manager@test.com")
                .fullName("Content Manager")
                .password("encodedpass")
                .role(Role.CONTENT_MANAGER)
                .build();
        userRepository.save(contentManager);

        normalUser = User.builder()
                .email("user@test.com")
                .fullName("Normal User")
                .password("encodedpass")
                .role(Role.USER)
                .build();
        userRepository.save(normalUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void testFindAllByRole() {
        // Find only Content Managers
        List<User> managers = userRepository.findAllByRole(Role.CONTENT_MANAGER);

        assertThat(managers).hasSize(1);
        assertThat(managers.get(0).getEmail()).isEqualTo("manager@test.com");
        assertThat(managers.get(0).getRole()).isEqualTo(Role.CONTENT_MANAGER);

        // Find users
        List<User> users = userRepository.findAllByRole(Role.USER);
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("user@test.com");
    }

    @Test
    void testFindByIdAndRole() {
        // Success case
        Optional<User> foundManager = userRepository.findByIdAndRole(contentManager.getId(), Role.CONTENT_MANAGER);
        assertThat(foundManager).isPresent();
        assertThat(foundManager.get().getEmail()).isEqualTo("manager@test.com");

        // Fail case: Find manager ID but check for ADMIN role
        Optional<User> wrongRole = userRepository.findByIdAndRole(contentManager.getId(), Role.ADMIN);
        assertThat(wrongRole).isEmpty();
    }

    @Test
    void testFindByEmailAndRole() {
        // Success case
        Optional<User> foundAdmin = userRepository.findByEmailAndRole("admin@test.com", Role.ADMIN);
        assertThat(foundAdmin).isPresent();
        assertThat(foundAdmin.get().getFullName()).isEqualTo("Admin");

        // Fail case: Find admin email but check for USER role
        Optional<User> wrongRole = userRepository.findByEmailAndRole("admin@test.com", Role.USER);
        assertThat(wrongRole).isEmpty();
    }
}
