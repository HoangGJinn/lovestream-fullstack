package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {
    // Kiểm tra xem mã Voucher đã tồn tại chưa
    boolean existsByCode(String code);

    // Tìm Voucher theo mã chính xác
    Optional<Voucher> findByCode(String code);

    // THÊM MỚI: Tìm Voucher theo từ khóa (Dùng cho thanh Search)
    List<Voucher> findByCodeContainingIgnoreCase(String code);

    @Modifying
    @Query("UPDATE Voucher v SET v.usedQuantity = v.usedQuantity + 1 WHERE v.id = :id AND v.usedQuantity < v.totalQuantity")
    int incrementUsedQuantity(@Param("id") String id);

    @Query("SELECT v FROM Voucher v WHERE v.status = 'ACTIVE' AND v.expiryDate >= CURRENT_DATE AND v.usedQuantity < v.totalQuantity")
    List<Voucher> findAvailableVouchers();
}


// model hết quota?? ???
// cái lồn gì mới xài có 5 prompt hết quot


//còn quota; cc cho nó chj