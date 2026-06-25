package com.hcmute.lovestream.service.authentication;

import com.hcmute.lovestream.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FacebookLoginProcessor extends SocialLoginProcessor {

    public FacebookLoginProcessor(UserRepository userRepository) {
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

    @SuppressWarnings("unchecked")
    @Override
    protected String extractAvatar(Map<String, Object> attributes) {
        if (attributes.containsKey("picture")) {
            Object pictureObj = attributes.get("picture");
            if (pictureObj instanceof Map) {
                Map<String, Object> pictureMap = (Map<String, Object>) pictureObj;
                Object dataObj = pictureMap.get("data");
                if (dataObj instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                    return (String) dataMap.get("url");
                }
            }
        }
        return null;
    }
}
