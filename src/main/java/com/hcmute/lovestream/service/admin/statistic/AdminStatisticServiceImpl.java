package com.hcmute.lovestream.service.admin.statistic.impl;

import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.repository.PaymentRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.service.admin.statistic.AdminStatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatisticServiceImpl implements AdminStatisticService {

        private final PaymentRepository paymentRepository;
        private final UserRepository userRepository;

        @Override
        @Transactional(readOnly = true)
        public Map<String, Object> getDashboardStatistics() {
                List<Payment> allPayments = paymentRepository.findAll();
                long totalUsers = userRepository.count();

                // 1. Lọc ra các giao dịch thành công
                List<Payment> successPayments = allPayments.stream()
                                .filter(p -> p.getStatus() != null &&
                                                (p.getStatus().name().equals("SUCCESS")
                                                                || p.getStatus().name().equals("COMPLETED")))
                                .collect(Collectors.toList());

                // 2. Tính Tổng doanh thu
                double totalRevenue = successPayments.stream()
                                .mapToDouble(p -> p.getAmount() != null ? p.getAmount().doubleValue() : 0.0)
                                .sum();

                // 3. Phân phối gói dịch vụ (Cho Doughnut Chart)
                Map<String, Long> planDistribution = successPayments.stream()
                                .filter(p -> p.getServicePlan() != null)
                                .collect(Collectors.groupingBy(p -> p.getServicePlan().getName(),
                                                Collectors.counting()));

                // 4. Doanh thu theo tháng trong năm nay (Cho Line Chart)
                int currentYear = LocalDate.now().getYear();
                Map<Integer, Double> revenueByMonthMap = successPayments.stream()
                                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().getYear() == currentYear)
                                .collect(Collectors.groupingBy(
                                                p -> p.getCreatedAt().getMonthValue(),
                                                Collectors.summingDouble(
                                                                p -> p.getAmount() != null ? p.getAmount().doubleValue()
                                                                                : 0.0)));

                // Đổ mảng 12 tháng (Tháng nào không có doanh thu thì gán = 0)
                List<Double> monthlyRevenue = new ArrayList<>();
                for (int i = 1; i <= 12; i++) {
                        monthlyRevenue.add(revenueByMonthMap.getOrDefault(i, 0.0));
                }

                // 5. Lấy 5 giao dịch mới nhất cho bảng dưới cùng
                List<Map<String, Object>> recentTransactions = successPayments.stream()
                                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                                .limit(5)
                                .map(p -> Map.<String, Object>of(
                                                "id", p.getId(),
                                                "userId", p.getUser() != null ? p.getUser().getId() : "N/A",
                                                "planName",
                                                p.getServicePlan() != null ? p.getServicePlan().getName() : "Custom",
                                                "amount", p.getAmount(),
                                                "paymentDate", p.getCreatedAt(),
                                                "status", p.getStatus().name()))
                                .collect(Collectors.toList());

                // Đóng gói tất cả vào 1 cục JSON duy nhất
                return Map.of(
                                "kpi", Map.of(
                                                "totalRevenue", totalRevenue,
                                                "totalUsers", totalUsers,
                                                "successfulTransactions", successPayments.size(),
                                                "activeSubscriptions",
                                                planDistribution.values().stream().mapToLong(Long::longValue).sum()),
                                "charts", Map.of(
                                                "monthlyRevenue", monthlyRevenue,
                                                "planDistribution", planDistribution),
                                "recentTransactions", recentTransactions);
        }
}