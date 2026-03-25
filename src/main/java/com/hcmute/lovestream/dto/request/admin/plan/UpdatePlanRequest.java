package com.hcmute.lovestream.dto.request.admin.plan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePlanRequest {

    // Exception 6.2: Bỏ trống thông tin
    @NotBlank(message = "Vui lòng điền đầy đủ thông tin bắt buộc")
    private String name;

    // Exception 6.1: Nhập sai định dạng (chỉ nhận số dương)
    @NotNull(message = "Vui lòng điền đầy đủ thông tin bắt buộc")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá tiền không hợp lệ")
    private BigDecimal price;
}