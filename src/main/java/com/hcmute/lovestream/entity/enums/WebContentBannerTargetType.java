package com.hcmute.lovestream.entity.enums;

public enum WebContentBannerTargetType {
    NONE("Không điều hướng"),
    MOVIE("Phim"),
    SERIES("Series"),
    STATIC_PAGE("Trang tĩnh"),
    EXTERNAL_URL("URL tùy chỉnh");

    private final String displayLabel;

    WebContentBannerTargetType(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
}
