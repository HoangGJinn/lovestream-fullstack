package com.hcmute.lovestream.entity.enums;

public enum AssetType {
    POSTER,
    TRAILER,
    BACKGROUND,
    /**
     * Legacy value that may exist in DB rows (e.g. imported seed data).
     * Prefer using {@link #MOVIE_VIDEO} or {@link #EPISODE_VIDEO} for new records.
     */
    FULL_VIDEO,
    MOVIE_VIDEO,
    EPISODE_VIDEO
}
