package com.hcmute.lovestream.service.authentication;

import com.hcmute.lovestream.dto.request.*;

import java.util.Map;

public interface AuthService {
    void register(Register request);
    void verifyEmail(String token);
    Map<String, String> login(Login request);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    void resendOtp(String email);
    Map<String, String> refreshToken(String refreshToken);
    void logout(String refreshToken);
}
