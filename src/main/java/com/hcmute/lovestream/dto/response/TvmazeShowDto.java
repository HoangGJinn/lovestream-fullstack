package com.hcmute.lovestream.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class TvmazeShowDto {
    private String name;
    private String summary;
    private String premiered;
    private Integer runtime;
    private ImageDto image;

    private List<String> genres;

    @Data
    public static class ImageDto {
        private String original;
    }
}