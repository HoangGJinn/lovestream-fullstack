package com.hcmute.lovestream.service.authentication;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.UserStatus;
import com.hcmute.lovestream.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SocialLoginTemplateTest {

    private UserRepository userRepository;
    private GoogleLoginProcessor googleLoginProcessor;
    private FacebookLoginProcessor facebookLoginProcessor;

    @BeforeEach
    public void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        googleLoginProcessor = new GoogleLoginProcessor(userRepository);
        facebookLoginProcessor = new FacebookLoginProcessor(userRepository);
    }

    @Test
    public void testGoogleLoginNewUser() {
        Map<String, Object> attributes = Map.of(
                "email", "alice@gmail.com",
                "name", "Alice Google",
                "picture", "http://google.com/alice.jpg"
        );

        when(userRepository.findByEmail("alice@gmail.com")).thenReturn(Optional.empty());

        User user = googleLoginProcessor.process(attributes);

        assertNotNull(user);
        assertEquals("alice@gmail.com", user.getEmail());
        assertEquals("Alice Google", user.getFullName());
        assertEquals("http://google.com/alice.jpg", user.getAvatar());
        assertTrue(user.isActive());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testFacebookLoginNewUser() {
        Map<String, Object> pictureData = Map.of("url", "http://facebook.com/bob.jpg");
        Map<String, Object> picture = Map.of("data", pictureData);
        Map<String, Object> attributes = Map.of(
                "email", "bob@fb.com",
                "name", "Bob Facebook",
                "picture", picture
        );

        when(userRepository.findByEmail("bob@fb.com")).thenReturn(Optional.empty());

        User user = facebookLoginProcessor.process(attributes);

        assertNotNull(user);
        assertEquals("bob@fb.com", user.getEmail());
        assertEquals("Bob Facebook", user.getFullName());
        assertEquals("http://facebook.com/bob.jpg", user.getAvatar());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testSocialLoginBannedUser() {
        Map<String, Object> attributes = Map.of(
                "email", "banned@gmail.com",
                "name", "Banned User",
                "picture", "http://google.com/banned.jpg"
        );

        User existingUser = new User();
        existingUser.setEmail("banned@gmail.com");
        existingUser.setStatus(UserStatus.BANNED);

        when(userRepository.findByEmail("banned@gmail.com")).thenReturn(Optional.of(existingUser));

        assertThrows(OAuth2AuthenticationException.class, () -> {
            googleLoginProcessor.process(attributes);
        });

        verify(userRepository, never()).save(any(User.class));
    }
}
