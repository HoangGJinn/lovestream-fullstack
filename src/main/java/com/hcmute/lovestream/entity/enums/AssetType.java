package com.hcmute.lovestream.entity.enums;

public enum AssetType {
    POSTER, TRAILER, BACKGROUND, MOVIE_VIDEO, EPISODE_VIDEO,
    
    @Deprecated // Giữ lại để tương thích với dữ liệu cũ trong DB (media_asset.asset_type = 'FULL_VIDEO')
    FULL_VIDEO
}
