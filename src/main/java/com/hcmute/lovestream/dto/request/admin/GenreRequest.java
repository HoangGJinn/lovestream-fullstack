package com.hcmute.lovestream.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreRequest {
    
    @NotBlank(message = "Tên thể loại không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂÂÊÔƠƯ\\u00C0-\\u024F\\u1E00-\\u1EFF]+$", 
             message = "Tên thể loại chỉ được chứa chữ cái, số, khoảng trắng và dấu gạch ngang")
    private String name;
}
