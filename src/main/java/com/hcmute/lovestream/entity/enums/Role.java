package com.hcmute.lovestream.entity.enums;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    USER,
    VIP,
    CONTENT_MANAGER,
    ADMIN;

    // Tự động thêm tiền tố ROLE_ vào khi Spring Security cần lấy quyền
    @Override
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}