package com.hcmute.lovestream.entity.enums;

public enum AssetType {
    POSTER,
    TRAILER,
    BACKGROUND,
    MOVIE_VIDEO,
    EPISODE_VIDEO,
    // Một số dữ liệu trong DB đang lưu dạng này (ví dụ: FULL_VIDEO),
    // nên cần có để tránh crash khi hydrate entity.
    FULL_VIDEO
}
