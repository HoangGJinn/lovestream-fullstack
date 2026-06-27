package com.hcmute.lovestream.service.payment.command;

import com.hcmute.lovestream.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UseVoucherCommand implements CompensatingCommand {

    private final VoucherRepository voucherRepository;
    private final String voucherId;
    
    private boolean executedSuccessfully = false;

    @Override
    public void execute() {
        if (voucherId == null || voucherId.isEmpty()) {
            return; // Không có voucher thì không cần làm gì
        }

        int updatedRows = voucherRepository.incrementUsedQuantity(voucherId);
        if (updatedRows == 0) {
            // Ném Exception sẽ chặn luồng, kích hoạt quá trình gọi undo() ở catch block của Invoker
            throw new RuntimeException("Failed to use voucher (maybe expired or out of stock): " + voucherId);
        }
        
        executedSuccessfully = true;
        log.info("[Command] Executed UseVoucherCommand: Incremented used quantity for {}", voucherId);
    }

    @Override
    public void undo() {
        if (!executedSuccessfully) {
            return; // Nếu chưa trừ thành công thì không cần cộng lại
        }
        
        voucherRepository.decrementUsedQuantity(voucherId);
        log.info("[Command] Undo UseVoucherCommand: Decremented used quantity for {}", voucherId);
    }
}
