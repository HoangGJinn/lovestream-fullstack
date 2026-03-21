package com.hcmute.lovestream.entity.enums;

public enum TransactionStatus {
    PENDING,   // Đang chờ thanh toán
    SUCCESS,   // Thanh toán thành công
    FAILED,    // Thanh toán thất bại
    REFUNDED   // Đã hoàn tiền
}