package com.hcmute.lovestream.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRoomRequest {

    @NotBlank(message = "Vui long nhap ten phong")
    private String roomName;

    @NotBlank(message = "Vui long chon phim")
    private String videoContentId;

    @NotNull(message = "Vui long chon che do phong")
    private Boolean privateRoom = Boolean.FALSE;

    private String password;

    @Min(value = 2, message = "So luong toi thieu la 2")
    @Max(value = 200, message = "So luong toi da la 200")
    private Integer maxParticipants = 10;

    @AssertTrue(message = "Vui long nhap mat khau khi tao phong rieng tu")
    public boolean isPasswordProvidedForPrivateRoom() {
        if (!Boolean.TRUE.equals(privateRoom)) {
            return true;
        }
        return password != null && !password.trim().isEmpty();
    }
}

