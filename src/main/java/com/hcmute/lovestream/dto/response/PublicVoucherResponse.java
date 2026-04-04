package com.hcmute.lovestream.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicVoucherResponse {
    private String code;
    private Integer discountPercent;
    private LocalDate expiryDate;
}
