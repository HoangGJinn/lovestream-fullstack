package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Mã Voucher (VD: NEWUSER50) - Bắt buộc nhập và không được trùng lặp
    @Column(unique = true, nullable = false, length = 50)
    private String code;

    // Phần trăm giảm giá (VD: 50)
    @Column(nullable = false)
    private Integer discountPercent;

    // Tổng số lượng mã được phát hành (VD: 1000)
    @Column(nullable = false)
    private Integer totalQuantity;

    // Số lượng mã đã được user sử dụng (Mặc định khi tạo mới là 0)
    @Column(nullable = false)
    @Builder.Default
    private Integer usedQuantity = 0;

    // Ngày hết hạn của Voucher
    @Column(nullable = false)
    private LocalDate expiryDate;

    // Trạng thái của Voucher (Hoạt động / Khóa)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;


     @OneToMany(mappedBy = "voucher")
     @Builder.Default
     @ToString.Exclude
     @EqualsAndHashCode.Exclude
     private List<Payment> payments = new ArrayList<>();
}