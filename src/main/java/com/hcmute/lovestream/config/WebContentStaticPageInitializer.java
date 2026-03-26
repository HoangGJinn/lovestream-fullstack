package com.hcmute.lovestream.config;

import com.hcmute.lovestream.entity.StaticPage;
import com.hcmute.lovestream.entity.enums.WebStaticPageType;
import com.hcmute.lovestream.repository.StaticPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebContentStaticPageInitializer implements CommandLineRunner {

    private final StaticPageRepository staticPageRepository;

    @Override
    public void run(String... args) {
        ensureDefaultPageIfMissing(WebStaticPageType.ABOUT,
                "<h2>Giới thiệu về LoveStream</h2>"
                        + "<p>LoveStream là nền tảng xem phim trực tuyến, giúp bạn khám phá nhiều thể loại hấp dẫn và trải nghiệm mượt mà trên mọi thiết bị.</p>"
                        + "<ul>"
                        + "<li>Xem phim/series theo danh mục gợi ý</li>"
                        + "<li>Lưu danh sách yêu thích &amp; theo dõi lịch sử xem</li>"
                        + "<li>Cập nhật nội dung mới thường xuyên</li>"
                        + "</ul>");

        ensureDefaultPageIfMissing(WebStaticPageType.PRIVACY_POLICY,
                "<h2>Chính sách bảo mật</h2>"
                        + "<p>Chúng tôi cam kết bảo vệ thông tin cá nhân của bạn. Dữ liệu chỉ được sử dụng cho mục đích vận hành dịch vụ và cải thiện trải nghiệm.</p>"
                        + "<ul>"
                        + "<li>Thu thập dữ liệu đăng nhập và thông tin hồ sơ cơ bản</li>"
                        + "<li>Sử dụng cookie/phân tích để tối ưu giao diện</li>"
                        + "<li>Không chia sẻ thông tin cho bên thứ ba trái phép</li>"
                        + "</ul>");

        ensureDefaultPageIfMissing(WebStaticPageType.TERMS,
                "<h2>Điều khoản sử dụng</h2>"
                        + "<p>Bằng việc truy cập và sử dụng LoveStream, bạn đồng ý tuân thủ các điều khoản sau:</p>"
                        + "<ol>"
                        + "<li>Tôn trọng nội dung và bản quyền</li>"
                        + "<li>Không sử dụng dịch vụ cho mục đích trái pháp luật</li>"
                        + "<li>Người dùng chịu trách nhiệm cho hoạt động tài khoản của mình</li>"
                        + "</ol>"
                        + "<p>Chúng tôi có thể cập nhật điều khoản theo thời gian.</p>");
    }

    private void ensureDefaultPageIfMissing(WebStaticPageType type, String defaultHtml) {
        if (type == null) {
            throw new IllegalArgumentException("Loại trang tĩnh không hợp lệ.");
        }
        if (defaultHtml == null) {
            defaultHtml = "";
        }
        if (staticPageRepository.existsByPageType(type)) {
            return;
        }
        java.util.Objects.requireNonNull(staticPageRepository.save(StaticPage.builder()
                .pageType(type)
                .htmlContent(defaultHtml)
                .build()));
        log.info("Created default static page: {}", type);
    }
}

