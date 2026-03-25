package com.hcmute.lovestream.service.admin.transaction;

import java.util.List;
import java.util.Map;

public interface AdminPaymentService {

    // ĐÃ XÓA HÀM CŨ: getCurrentMonthTransactions()

    // ĐÃ THÊM HÀM MỚI (Có chứa các tham số bộ lọc):
    List<Map<String, Object>> getFilteredTransactions(String fromDate, String toDate, String plan, String status);

}