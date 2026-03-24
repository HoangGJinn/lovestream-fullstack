package com.hcmute.lovestream.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class StatisticResponseDTO {
    // 1. Nhóm chỉ số KPI (Các thẻ số to trên cùng)
    private double totalRevenue;
    private long totalUsers;
    private long totalTransactions;
    private long activeSubscribers;

    // 2. Nhóm dữ liệu biểu đồ
    // Map chứa tên gói và số lượng tương ứng (VD: "Premium" -> 250, "Basic" -> 500)
    private Map<String, Long> planDistribution;

    // Map chứa Ngày và Doanh thu ngày đó (VD: "2026-03-20" -> 150.0)
    private Map<String, Double> revenueTrend;

    // 3. Nhóm bảng phụ (Giao dịch mới nhất)
    private List<TransactionRecord> recentTransactions;

    // Class nội bộ để chứa thông tin 1 dòng giao dịch
    @Data
    @Builder
    public static class TransactionRecord {
        private String userId;
        private String fullName;
        private String planName;
        private double amount;
        private String timestamp;
        private String status;
    }
}