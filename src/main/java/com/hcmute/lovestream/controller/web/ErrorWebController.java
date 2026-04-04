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

        String title = resolveTitle(statusCode);
        String message = resolveMessage(statusCode);

        if (messageObj instanceof String rawMessage && !rawMessage.isBlank()) {
            model.addAttribute("systemMessage", rawMessage);
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", message);
        model.addAttribute("homeUrl", "/home");
        return resolveView(statusCode);
    }

    private String resolveView(int statusCode) {
        return switch (statusCode) {
            case 300 -> "error/300";
            case 400 -> "error/400";
            case 401 -> "error/401";
            case 403 -> "error/403";
            case 404 -> "error/404";
            case 500 -> "error/500";
            default -> {
                if (statusCode >= 300 && statusCode < 400) {
                    yield "error/3xx";
                }
                if (statusCode >= 400 && statusCode < 500) {
                    yield "error/4xx";
                }
                if (statusCode >= 500 && statusCode < 600) {
                    yield "error/5xx";
                }
                yield "error/500";
            }
        };
    }

    private String resolveTitle(int statusCode) {
        return switch (statusCode) {
            case 300 -> "Nhiều hướng chuyển tiếp";
            case 400 -> "Yêu cầu không hợp lệ";
            case 401 -> "Bạn chưa đăng nhập";
            case 403 -> "Bạn không có quyền truy cập";
            case 404 -> "Không tìm thấy trang";
            case 500 -> "Lỗi hệ thống";
            default -> {
                if (statusCode >= 300 && statusCode < 400) {
                    yield "Lỗi chuyển hướng";
                }
                if (statusCode >= 400 && statusCode < 500) {
                    yield "Yêu cầu không thể xử lý";
                }
                if (statusCode >= 500 && statusCode < 600) {
                    yield "Máy chủ đang gặp sự cố";
                }
                yield "Đã xảy ra lỗi";
            }
        };
    }

    private String resolveMessage(int statusCode) {
        return switch (statusCode) {
            case 300 -> "Yêu cầu của bạn có nhiều lựa chọn phản hồi. Vui lòng thử lại hoặc quay về trang chủ.";
            case 400 -> "Dữ liệu gửi lên chưa đúng định dạng hoặc thiếu thông tin bắt buộc.";
            case 401 -> "Vui lòng đăng nhập để tiếp tục sử dụng tính năng này.";
            case 403 -> "Tài khoản hiện tại không đủ quyền để truy cập nội dung này.";
            case 404 -> "Nội dung bạn truy cập không tồn tại hoặc đã được di chuyển.";
            case 500 -> "Hệ thống đang gặp lỗi nội bộ. Vui lòng thử lại sau.";
            default -> {
                if (statusCode >= 300 && statusCode < 400) {
                    yield "Yêu cầu đã bị chuyển hướng bất thường. Vui lòng thử lại.";
                }
                if (statusCode >= 400 && statusCode < 500) {
                    yield "Yêu cầu không hợp lệ hoặc không được phép thực hiện.";
                }
                if (statusCode >= 500 && statusCode < 600) {
                    yield "Dịch vụ tạm thời gián đoạn. Vui lòng thử lại sau.";
                }
                yield "Đã xảy ra lỗi. Vui lòng thử lại.";
            }
        };
    }
}



