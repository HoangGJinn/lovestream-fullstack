package com.hcmute.lovestream.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmail {
    @NotBlank(message = "Mã xác minh không được để trống")
    private String token;
}
