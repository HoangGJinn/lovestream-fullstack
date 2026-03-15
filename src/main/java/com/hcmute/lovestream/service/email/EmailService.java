package com.hcmute.lovestream.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    /**
     * Gửi email chứa mã OTP Xác nhận đăng ký
     */
    @Async
    public void sendVerificationEmail(String toEmail, String otp) {
        String subject = "Mã xác nhận đăng ký tài khoản LoveStream";
        String htmlContent = buildEmailTemplate(
                "Xác nhận địa chỉ email",
                "Cảm ơn bạn đã tham gia LoveStream! Để hoàn tất việc đăng ký và bắt đầu khám phá hàng ngàn bộ phim đỉnh cao, vui lòng nhập mã xác nhận bên dưới:",
                otp
        );
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Gửi email chứa mã OTP Quên mật khẩu
     */
    @Async
    public void sendResetPasswordEmail(String toEmail, String otp) {
        String subject = "Yêu cầu đặt lại mật khẩu LoveStream";
        String htmlContent = buildEmailTemplate(
                "Đặt lại mật khẩu của bạn",
                "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản LoveStream của bạn. Sử dụng mã xác nhận dưới đây để tiếp tục. Mã này sẽ hết hạn sau 5 phút:",
                otp
        );
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Hàm dùng chung để gửi email HTML
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // Set true để Spring Boot hiểu đây là HTML

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi khi gửi email: " + e.getMessage());
        }
    }

    /**
     * Giao diện Email HTML (Tone Đỏ - Đen chuẩn Netflix/LoveStream)
     */
    private String buildEmailTemplate(String title, String message, String otp) {
        return "<div style=\"font-family: Arial, sans-serif; background-color: #141414; color: #ffffff; padding: 40px 20px; text-align: center;\">"
                + "<div style=\"max-width: 500px; margin: auto; background-color: #000000; padding: 30px; border-radius: 8px; border: 1px solid #333;\">"
                + "<h1 style=\"color: #e50914; margin-bottom: 20px; font-weight: 900; letter-spacing: -1px;\">LoveStream</h1>"
                + "<h2 style=\"color: #ffffff; font-size: 20px;\">" + title + "</h2>"
                + "<p style=\"color: #b3b3b3; font-size: 15px; line-height: 1.5; margin-bottom: 30px;\">" + message + "</p>"
                + "<div style=\"font-size: 32px; font-weight: bold; color: #ffffff; background-color: #333; padding: 15px; border-radius: 4px; letter-spacing: 5px; margin-bottom: 30px;\">"
                + otp
                + "</div>"
                + "<p style=\"color: #737373; font-size: 12px;\">Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này. Tài khoản của bạn vẫn an toàn.</p>"
                + "</div>"
                + "</div>";
    }
}