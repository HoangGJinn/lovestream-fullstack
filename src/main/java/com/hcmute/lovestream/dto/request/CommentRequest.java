package com.hcmute.lovestream.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank(message = "Bình luận không được để trống")
    @Size(max = 500, message = "Bình luận quá dài tối đa 500 ký tự")
    private String content;

    private String videoContentId;
    private String episodeId;
    private String parentCommentId;
}
