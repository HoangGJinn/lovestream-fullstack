package com.hcmute.lovestream.service.admin.transaction;

import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFilteredTransactions(String fromDate, String toDate, String plan, String status) {

        LocalDateTime start;
        LocalDateTime end;

        // 1. Xử lý thời gian (Lọc bằng DB cho nhẹ)
        if (fromDate != null && !fromDate.isBlank() && toDate != null && !toDate.isBlank()) {
            start = LocalDate.parse(fromDate).atStartOfDay(); // 00:00:00 của ngày bắt đầu
            end = LocalDate.parse(toDate).atTime(23, 59, 59); // 23:59:59 của ngày kết thúc
        } else {
            // Mặc định lấy tháng này nếu không chọn ngày
            YearMonth currentMonth = YearMonth.now();
            start = currentMonth.atDay(1).atStartOfDay();
            end = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        }

        // Kéo danh sách giao dịch trong khoảng thời gian từ DB
        List<Payment> payments = paymentRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);

        // 2. Lọc tiếp theo Gói và Trạng thái (Dùng Java Stream)
        return payments.stream()
                .filter(p -> {
                    // Lọc theo Gói (Nếu UI gửi chữ "ALL" thì bỏ qua lọc)
                    boolean matchPlan = plan == null || plan.equals("ALL") ||
                            (p.getServicePlan() != null && p.getServicePlan().getName().equalsIgnoreCase(plan));

                    // Lọc theo Trạng thái
                    boolean matchStatus = status == null || status.equals("ALL") ||
                            (p.getStatus() != null && p.getStatus().name().equalsIgnoreCase(status));

                    return matchPlan && matchStatus;
                })
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "userId", p.getUser() != null ? p.getUser().getId() : "N/A",
                        "planName", p.getServicePlan() != null ? p.getServicePlan().getName() : "Không xác định",
                        "amount", p.getAmount(),
                        "paymentDate", p.getCreatedAt(),
                        "status", p.getStatus() != null ? p.getStatus().name() : "UNKNOWN"
                )).collect(Collectors.toList());
    }
}