package com.hcmute.lovestream.dto.request;

import com.hcmute.lovestream.entity.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    private String fullName;

    @Pattern(
            regexp = "^$|^[0-9+\\-\\s]{8,15}$",
            message = "Số điện thoại không hợp lệ"
    )
    private String phone;

    private Gender gender;

    @Size(max = 500, message = "Link avatar quá dài")
    private String avatar;
}
