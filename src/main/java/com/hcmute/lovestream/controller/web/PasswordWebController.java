package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.request.BackupChangePasswordRequest;
import com.hcmute.lovestream.dto.request.ChangePasswordRequest;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.service.user.ChangePasswordService;
import com.hcmute.lovestream.service.user.ChangePasswordService.BackupTokenStatus;
import com.hcmute.lovestream.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PasswordWebController {

    private final UserProfileService userProfileService;
    private final ChangePasswordService changePasswordService;

    @GetMapping("/account/change-password")
    public String changePasswordPage(Authentication authentication, Model model) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        model.addAttribute("currentUser", currentUser);

        model.addAttribute("changePasswordForm", new ChangePasswordRequest());

        return "user/change-password";
    }

    @GetMapping("/account/change-password/backup")
    public String backupChangePasswordPage(@RequestParam("token") String token, Model model) {
        BackupTokenStatus status = changePasswordService.validateBackupToken(token);

        if (status == BackupTokenStatus.INVALID) {
            model.addAttribute("errorMessage", "Đường dẫn không hợp lệ");
        } else if (status == BackupTokenStatus.EXPIRED) {
            model.addAttribute("errorMessage", "Đường dẫn đã hết hạn");
        } else {
            BackupChangePasswordRequest form = new BackupChangePasswordRequest();
            form.setToken(token);
            model.addAttribute("backupChangePasswordForm", form);
        }

        return "auth/change-password-backup";
    }
}
