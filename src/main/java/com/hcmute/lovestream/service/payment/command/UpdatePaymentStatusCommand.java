package com.hcmute.lovestream.service.payment.command;

import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.entity.enums.TransactionStatus;
import com.hcmute.lovestream.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UpdatePaymentStatusCommand implements CompensatingCommand {

    private final PaymentRepository paymentRepository;
    private final Payment payment;
    private final TransactionStatus targetStatus;
    private final String transactionNo;

    // Lưu trạng thái trước khi execute để có thể undo
    private TransactionStatus previousStatus;
    private String previousTransactionNo;

    @Override
    public void execute() {
        this.previousStatus = payment.getStatus();
        this.previousTransactionNo = payment.getTransactionCode();

        payment.setStatus(targetStatus);
        
        // Cập nhật mã giao dịch từ cổng thanh toán (nếu có)
        if (transactionNo != null && !transactionNo.trim().isEmpty() && !"0".equals(transactionNo)) {
            payment.setTransactionCode(transactionNo);
        }

        paymentRepository.saveAndFlush(payment);
        log.info("[Command] Executed UpdatePaymentStatusCommand: {} -> {}", previousStatus, targetStatus);
    }

    @Override
    public void undo() {
        payment.setStatus(previousStatus);
        payment.setTransactionCode(previousTransactionNo);
        paymentRepository.saveAndFlush(payment);
        log.info("[Command] Undo UpdatePaymentStatusCommand: Reverted to {}", previousStatus);
    }
}
