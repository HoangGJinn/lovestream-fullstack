package com.hcmute.lovestream.service.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final GoogleLoginProcessor googleLoginProcessor;
    private final OidcUserService delegate = new OidcUserService();

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        try {
            googleLoginProcessor.process(oidcUser.getAttributes());
        } catch (Exception e) {
            log.error("Google login processing failed", e);
            if (e instanceof OAuth2AuthenticationException) {
                throw (OAuth2AuthenticationException) e;
            }
            throw new OAuth2AuthenticationException(e.getMessage());
        }
        return oidcUser;
    }
}
