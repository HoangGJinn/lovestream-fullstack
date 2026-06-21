package com.hcmute.lovestream.service.payment.command;

/**
 * Compensating Command Pattern (Saga Pattern)
 * 
 * Định nghĩa hợp đồng cho các tác vụ có khả năng hoàn tác (rollback).
 * Được sử dụng trong các chuỗi giao dịch phức tạp không thể phụ thuộc hoàn toàn
 * vào Database Transaction (ví dụ: giao dịch phân tán, tích hợp API ngoài).
 */
public interface CompensatingCommand {
    /**
     * Thực thi tác vụ chính.
     */
    void execute();

    /**
     * Hoàn tác tác vụ nếu có một tác vụ khác trong chuỗi bị lỗi.
     */
    void undo();
}
