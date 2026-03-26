package com.hcmute.lovestream.service.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum CloudinaryFolderTarget {
    CONTENT_BANNER("lovestream/content/banners", "image"),
    MOVIE_POSTER("lovestream/movies/posters", "image"),
    MOVIE_TRAILER("lovestream/movies/trailers", "video"),
    MOVIE_FULL_VIDEO("lovestream/movies/videos", "video"),
    SERIES_POSTER("lovestream/series/posters", "image"),
    SERIES_TRAILER("lovestream/series/trailers", "video"),
    EPISODE_VIDEO("lovestream/series/episodes", "video");

    private final String folderPath;
    private final String resourceType;

    public static List<String> folderCreationOrder() {
        Set<String> orderedFolders = new LinkedHashSet<>();

        for (CloudinaryFolderTarget target : values()) {
            String[] parts = target.folderPath.split("/");
            StringBuilder currentPath = new StringBuilder();
            for (String part : parts) {
                if (!currentPath.isEmpty()) {
                    currentPath.append('/');
                }
                currentPath.append(part);
                orderedFolders.add(currentPath.toString());
            }
        }

        return new ArrayList<>(orderedFolders);
    }
}
