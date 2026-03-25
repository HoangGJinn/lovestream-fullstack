package com.hcmute.lovestream.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    @ModelAttribute
    public void addGlobalAttributes(Authentication authentication, Model model) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            Object principal = authentication.getPrincipal();

            // 2. Lấy thông tin User từ Principal (Dữ liệu từ JWT đã được Filter xử lý)
            if (principal instanceof Map<?, ?> principalMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentUser = (Map<String, Object>) principalMap;

                // Đưa thông tin User lên mọi trang HTML qua biến "currentUser"
                model.addAttribute("currentUser", currentUser);

                model.addAttribute("hasActiveSub", Boolean.TRUE.equals(currentUser.get("isVip")));
            }
        } else {
            model.addAttribute("hasActiveSub", false);
        }
    }
}