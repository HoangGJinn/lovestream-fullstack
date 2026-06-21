package com.hcmute.lovestream.dto.request;

import com.hcmute.lovestream.entity.enums.SharePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareRequest {
    @NotBlank(message = "Video ID cannot be blank")
    private String videoId;
    
    @NotNull(message = "Platform cannot be null")
    private SharePlatform platform;
    
    // Only used when platform is EMAIL
    private String recipientEmail;
}
