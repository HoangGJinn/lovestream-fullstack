package com.hcmute.lovestream.service.admin;

import com.hcmute.lovestream.dto.request.VoucherCreateRequest;
import com.hcmute.lovestream.entity.Voucher;
import com.hcmute.lovestream.entity.enums.VoucherStatus;
import com.hcmute.lovestream.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional
    public void createVoucher(VoucherCreateRequest request) {
        String standardizedCode = request.getCode().trim().toUpperCase();

        if (voucherRepository.existsByCode(standardizedCode)) {
            throw new RuntimeException("Mã Voucher '" + standardizedCode + "' đã tồn tại trong hệ thống!");
        }

        Voucher newVoucher = Voucher.builder()
                .code(standardizedCode)
                .discountPercent(request.getDiscountPercent())
                .totalQuantity(request.getTotalQuantity())
                .usedQuantity(0)
                .expiryDate(request.getExpiryDate())
                // ĐÃ SỬA: Mặc định tạo ra là bị khóa (BLOCKED)
                .status(VoucherStatus.BLOCKED)
                .build();

        voucherRepository.save(newVoucher);
    }

    // THÊM MỚI: Logic query từ Database và format thành JSON cho Frontend
    @Override
    public List<Map<String, Object>> getAllVouchers(String search) {
        List<Voucher> vouchers;

        // Nếu có từ khóa search thì tìm theo từ khóa, không thì lấy tất cả
        if (search != null && !search.isBlank()) {
            vouchers = voucherRepository.findByCodeContainingIgnoreCase(search.trim());
        } else {
            vouchers = voucherRepository.findAll();
        }

        // ĐÃ SỬA CHỖ NÀY: Thêm <String, Object> vào trước of()
        return vouchers.stream().map(v -> Map.<String, Object>of(
                "code", v.getCode(),
                "discount", v.getDiscountPercent(),
                "used", v.getUsedQuantity(),
                "total", v.getTotalQuantity(),
                "expiry", v.getExpiryDate().toString(),
                "status", v.getStatus().name()
        )).collect(Collectors.toList());
    }
    @Override
    @Transactional
    public void toggleVoucherStatus(String code) {
        // 1. Tìm Voucher trong Database
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voucher với mã: " + code));

        // 2. Đảo ngược trạng thái
        if (voucher.getStatus() == VoucherStatus.ACTIVE) {
            voucher.setStatus(VoucherStatus.BLOCKED);
        } else {
            voucher.setStatus(VoucherStatus.ACTIVE);
        }

        // Nhờ có @Transactional, Hibernate sẽ tự động update xuống DB khi hàm này chạy xong
        // Không cần gọi voucherRepository.save(voucher)
    }

    @Override
    @Transactional
    public void duplicateVoucher(String sourceCode, String newCode) {
        String standardizedNewCode = newCode.trim().toUpperCase();

        if (voucherRepository.existsByCode(standardizedNewCode)) {
            throw new RuntimeException("Mã Voucher mới '" + standardizedNewCode + "' đã tồn tại trong hệ thống!");
        }

        Voucher sourceVoucher = voucherRepository.findByCode(sourceCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voucher gốc với mã: " + sourceCode));

        // Ứng dụng Prototype Pattern (Rất ngắn gọn!)
        Voucher clonedVoucher = sourceVoucher.clone();
        clonedVoucher.setCode(standardizedNewCode);

        voucherRepository.save(clonedVoucher);
    }
}