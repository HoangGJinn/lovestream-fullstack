package com.hcmute.lovestream.controller.web;

import com.hcmute.lovestream.dto.request.UpdateProfileRequest;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Gender;
import com.hcmute.lovestream.service.user.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class UserWebController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public String profilePage(Authentication authentication, Model model) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        model.addAttribute("currentUser", currentUser);
        return "user/profile";
    }

    @GetMapping("/profile/edit")
    public String editProfilePage(Authentication authentication, Model model) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("genderOptions", Gender.values());

        if (!model.containsAttribute("profileForm")) {
            UpdateProfileRequest profileForm = new UpdateProfileRequest();
            profileForm.setFullName(currentUser.getFullName());
            profileForm.setPhone(currentUser.getPhone());
            profileForm.setGender(currentUser.getGender());
            profileForm.setAvatar(currentUser.getAvatar());
            model.addAttribute("profileForm", profileForm);
        }

        return "user/edit-profile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(
            Authentication authentication,
            @Valid @ModelAttribute("profileForm") UpdateProfileRequest profileForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("genderOptions", Gender.values());

        if (bindingResult.hasErrors()) {
            return "user/edit-profile";
        }

        try {
            userProfileService.updateCurrentUserProfile(authentication.getName(), profileForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("phone", "phone.duplicate", ex.getMessage());
            return "user/edit-profile";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công");
        return "redirect:/profile/edit";
    }

    @GetMapping("/account")
    public String accountOverviewPage(Authentication authentication, Model model) {
        User currentUser = userProfileService.getCurrentUserByEmail(authentication.getName());
        model.addAttribute("currentUser", currentUser);
        return "user/account"; // Trỏ tới file templates/user/account.html
    }
}