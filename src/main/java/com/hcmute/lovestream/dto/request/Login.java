package com.hcmute.lovestream.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Login {

    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong hop le")
    private String email;

    @NotBlank(message = "Mat khau khong duoc de trong")
    private String password;

    @Size(max = 128, message = "deviceId toi da 128 ky tu")
    private String deviceId;
}
