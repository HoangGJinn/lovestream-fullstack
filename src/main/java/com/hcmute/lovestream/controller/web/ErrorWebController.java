package com.hcmute.lovestream.controller.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorWebController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageObj = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (statusObj != null) {
            try {
                statusCode = Integer.parseInt(statusObj.toString());
            } catch (NumberFormatException ignored) { }
        }

        String title = "Đã xảy ra lỗi";
        String message = "Đã xảy ra lỗi. Vui lòng thử lại.";

        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            title = "Không tìm thấy trang";
            message = "Nội dung bạn truy cập không tồn tại hoặc đã được di chuyển.";
        } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
            title = "Không có quyền truy cập";
            message = "Bạn không có quyền truy cập nội dung này.";
        } else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
            title = "Yêu cầu đăng nhập";
            message = "Vui lòng đăng nhập để tiếp tục.";
        }

        if (messageObj instanceof String rawMessage && !rawMessage.isBlank()) {
            model.addAttribute("systemMessage", rawMessage);
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", message);
        return "error";
    }
}



