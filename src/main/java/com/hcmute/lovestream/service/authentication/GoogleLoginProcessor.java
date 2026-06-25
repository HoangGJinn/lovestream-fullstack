package com.hcmute.lovestream.service.authentication;

import com.hcmute.lovestream.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GoogleLoginProcessor extends SocialLoginProcessor {

    public GoogleLoginProcessor(UserRepository userRepository) {
        super(userRepository);
    }

    @Override
    protected String extractEmail(Map<String, Object> attributes) {
        return (String) attributes.get("email");
    }

    @Override
    protected String extractFullName(Map<String, Object> attributes) {
        return (String) attributes.get("name");
    }

    @Override
    protected String extractAvatar(Map<String, Object> attributes) {
        return (String) attributes.get("picture");
    }
}
