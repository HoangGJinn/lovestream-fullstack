package com.hcmute.lovestream.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class VoucherCreateRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(min = 3, max = 20, message = "Mã voucher phải từ 3 đến 20 ký tự")
    private String code;

    @NotNull(message = "Vui lòng nhập phần trăm giảm giá")
    @Min(value = 1, message = "Giảm giá tối thiểu là 1%")
    @Max(value = 100, message = "Giảm giá tối đa là 100%")
    private Integer discountPercent;

    @NotNull(message = "Vui lòng nhập số lượng phát hành")
    @Min(value = 1, message = "Số lượng tối thiểu phải là 1")
    private Integer totalQuantity;

    @NotNull(message = "Vui lòng chọn ngày hết hạn")
    @FutureOrPresent(message = "Ngày hết hạn phải từ hôm nay trở đi")
    private LocalDate expiryDate;
}