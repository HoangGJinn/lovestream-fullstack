package com.hcmute.lovestream.security;

// Đừng quên import Enum này vào nhé
import com.hcmute.lovestream.entity.enums.UserStatus;
import com.hcmute.lovestream.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ActiveUserFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.isBlank()) {
            return true;
        }
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/assets/")
                || path.startsWith("/uploads/")
                || path.startsWith("/webjars/")
                || path.equals("/favicon.ico")
                || path.startsWith("/ws-lovestream");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Lấy thông tin người đang đăng nhập hiện tại
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String email = auth.getName(); // Lấy email người dùng

            // 2. Chọc xuống DB kiểm tra xem có vừa bị Admin khóa không
            userRepository.findByEmail(email).ifPresent(user -> {

                // 👉 ĐÃ SỬA: Kiểm tra Enum status thay vì isActive
                if (user.getStatus() != UserStatus.ACTIVE) {

                    // NẾU BỊ KHÓA: Hủy session, xóa Security Context ngay lập tức
                    SecurityContextHolder.clearContext();
                    if (request.getSession(false) != null) {
                        request.getSession(false).invalidate();
                    }

                    try {
                        // Thêm check null an toàn cho lý do khóa
                        String lockMsg = user.getLockReason() != null ? user.getLockReason() : "Tài khoản đã bị khóa do vi phạm chính sách.";

                        // 3. Đuổi ra màn hình đăng nhập và đính kèm lý do
                        String encodedReason = URLEncoder.encode(lockMsg, StandardCharsets.UTF_8);
                        response.sendRedirect("/login?error=locked&reason=" + encodedReason);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // Nếu bình thường (chưa bị khóa hoặc chưa đăng nhập) thì cho đi tiếp
        if (!response.isCommitted()) {
            filterChain.doFilter(request, response);
        }
    }
}
