package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.service.user.AccountDeletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/account/deletion")
@RequiredArgsConstructor
public class AccountDeletionRestController {

    private final AccountDeletionService accountDeletionService;

    @PostMapping("/request")
    public ResponseEntity<?> requestAccountDeletion(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Vui long dang nhap de thuc hien thao tac nay"
            ));
        }

        try {
            accountDeletionService.requestAccountDeletion(authentication.getName());
            return ResponseEntity.ok(Map.of(
                    "message", "Da gui email xac nhan xoa tai khoan. Vui long kiem tra hop thu."
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Khong the gui yeu cau xoa tai khoan luc nay"
            ));
        }
    }
}
