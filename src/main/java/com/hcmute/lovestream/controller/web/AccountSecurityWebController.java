package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.service.user.AccountDeletionService;
import com.hcmute.lovestream.service.user.UserProfileService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AccountSecurityWebController {

    private final UserProfileService userProfileService;
    private final AccountDeletionService accountDeletionService;

    @GetMapping("/account/security")
    public String securityPage(Authentication authentication, Model model) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        model.addAttribute("currentUserDetail", currentUser);
        model.addAttribute("canDeleteAccount", currentUser.getRole() != null && currentUser.getRole().name().equals("USER"));
        return "user/security";
    }

    @GetMapping("/account/devices")
    public String devicesPage() {
        return "user/devices";
    }

    @GetMapping("/account/delete/confirm")
    public String confirmDeleteAccount(
            @RequestParam("token") String token,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        AccountDeletionService.DeletionResult result = accountDeletionService.confirmAccountDeletion(token);

        switch (result) {
            case SUCCESS -> {
                clearAuthCookies(response);
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
                model.addAttribute("success", true);
                model.addAttribute("title", "Xoa tai khoan thanh cong");
                model.addAttribute("message", "Tai khoan cua ban da duoc chuyen sang trang thai REMOVED.");
            }
            case EXPIRED -> {
                model.addAttribute("success", false);
                model.addAttribute("title", "Lien ket da het han");
                model.addAttribute("message", "Vui long quay lai trang Bao mat de gui lai yeu cau xoa tai khoan.");
            }
            default -> {
                model.addAttribute("success", false);
                model.addAttribute("title", "Lien ket khong hop le");
                model.addAttribute("message", "Lien ket co the da duoc su dung hoac khong ton tai.");
            }
        }

        return "auth/account-deletion-result";
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("JWT_TOKEN", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        Cookie refreshCookie = new Cookie("REFRESH_TOKEN", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }
}
