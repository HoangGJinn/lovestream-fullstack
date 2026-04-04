package com.hcmute.lovestream.service.admin.user;

import com.hcmute.lovestream.dto.request.admin.user.CreateContentManagerRequest;
import com.hcmute.lovestream.entity.User;

import java.util.List;
import java.util.Map;

public interface AdminUserManagementService {
    
    /**
     * Tạo tài khoản mới dành riêng cho Role CONTENT_MANAGER.
     * Tài khoản tạo qua kênh admin sẽ lập tức kích hoạt mà không cần chờ OTP.
     * 
     * @param request thông tin Content Manager cần tạo
     * @return User Object của nhân sự vừa được khởi tạo
     */
    // Lấy danh sách User trong hệ thống
    List<Map<String, Object>> getAllUsers(String searchKeyword);
    // Tạo tài khoản cho CM
    User createContentManager(CreateContentManagerRequest request);
    // Khóa/Mở tài khoản
    void toggleUserLock(String id, String reason);
}
