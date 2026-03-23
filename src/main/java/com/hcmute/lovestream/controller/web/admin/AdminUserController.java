package com.hcmute.lovestream.controller.web.admin;

import com.hcmute.lovestream.dto.request.admin.user.CreateContentManagerRequest;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.service.admin.user.AdminUserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final AdminUserManagementService adminUserManagementService;

    // 1. Hiển thị danh sách Content Manager
    @GetMapping("/content-managers")
    public String listContentManagers(Model model) {
        List<User> managers = userRepository.findAllByRole(Role.CONTENT_MANAGER);
        model.addAttribute("managers", managers);
        return "admin/users/content-managers/list";
    }

    // 2. Hiển thị form tạo mới
    @GetMapping("/content-managers/new")
    public String showCreateContentManagerForm(Model model) {
        if (!model.containsAttribute("createContentManagerRequest")) {
            model.addAttribute("createContentManagerRequest", CreateContentManagerRequest.builder().build());
        }
        return "admin/users/content-managers/form";
    }

    // 3. Xử lý tạo mới
    @PostMapping("/content-managers")
    public String createContentManager(
            @Valid @ModelAttribute("createContentManagerRequest") CreateContentManagerRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        // Bắt lỗi Validation (DTO Constraints)
        if (bindingResult.hasErrors()) {
            return "admin/users/content-managers/form";
        }

        try {
            // Gọi tầng Service để thực hiện Logic nghiệp vụ chuyên sâu
            adminUserManagementService.createContentManager(request);

            // Redirect về danh sách kèm thông báo Success
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản Content Manager thành công!");
            return "redirect:/admin/users/content-managers";

        } catch (RuntimeException e) {
            // Bắt lỗi Trùng lặp (từ Service ném ra) và dán cờ Lỗi vào Form Field cho người dùng biết
            if (e.getMessage() != null && e.getMessage().contains("Email")) {
                bindingResult.rejectValue("email", "error.user", e.getMessage());
            } else if (e.getMessage() != null && e.getMessage().contains("Số điện thoại")) {
                bindingResult.rejectValue("phone", "error.user", e.getMessage());
            } else {
                bindingResult.reject("error.user", e.getMessage());
            }
            return "admin/users/content-managers/form";
        }
    }
}
