package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {
    // Kiểm tra xem mã Voucher đã tồn tại chưa
    boolean existsByCode(String code);

    // Tìm Voucher theo mã chính xác
    Optional<Voucher> findByCode(String code);

    // THÊM MỚI: Tìm Voucher theo từ khóa (Dùng cho thanh Search)
    List<Voucher> findByCodeContainingIgnoreCase(String code);
}