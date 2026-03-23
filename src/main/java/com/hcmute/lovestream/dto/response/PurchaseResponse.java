package com.hcmute.lovestream.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {
    private String message;
    private String planName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
