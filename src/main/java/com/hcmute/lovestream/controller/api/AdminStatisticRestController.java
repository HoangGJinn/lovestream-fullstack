package com.hcmute.lovestream.controller.api;

import com.hcmute.lovestream.dto.response.StatisticResponseDTO;
// import com.hcmute.lovestream.service.admin.StatisticService; // Bạn sẽ tạo service này sau
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticRestController {

    // private final StatisticService statisticService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        try {
            // Thực tế bạn sẽ gọi:
            // StatisticResponseDTO stats = statisticService.getDashboardStats();

            // TẠM THỜI MOCK DATA ĐỂ TEST GIAO DIỆN TRƯỚC (Giả lập Service đã chạy xong)
            StatisticResponseDTO stats = StatisticResponseDTO.builder()
                    .totalRevenue(1250000.0)
                    .totalUsers(150000)
                    .totalTransactions(12000)
                    .activeSubscribers(80000)
                    .planDistribution(Map.of("Basic", 30L, "Standard", 15L, "Premium", 25L, "No Plan", 30L))
                    .revenueTrend(Map.of("1 Day", 50000.0, "5 Day", 100000.0, "15 Day", 150000.0, "30 Day", 180000.0))
                    .build();

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            // Chuẩn Exception Flow E1: Báo lỗi 500 và trả về JSON có key "error"
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Không thể tải dữ liệu thống kê lúc này. Vui lòng thử lại sau.",
                    "detail", e.getMessage()
            ));
        }
    }
}