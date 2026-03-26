package com.hcmute.lovestream.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hcmute.lovestream.config.properties.CloudinaryProperties;
import com.hcmute.lovestream.entity.enums.AssetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryMediaStorageService implements MediaStorageService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    /** Asset types that map to Cloudinary's "video" resource_type */
    private static final Set<AssetType> VIDEO_TYPES = Set.of(AssetType.FULL_VIDEO, AssetType.TRAILER,
            AssetType.EPISODE_VIDEO);

    @Override
    public String upload(MultipartFile file, AssetType assetType) throws IOException {
        String apiKey = cloudinaryProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("placeholder")) {
            throw new IllegalStateException(
                    "Cloudinary chưa được cấu hình. Vui lòng cập nhật cấu hình API key để sử dụng tính năng upload.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload không được để trống!");
        }

        String resourceType = VIDEO_TYPES.contains(assetType) ? "video" : "image";
        String folder = buildFolder(assetType);

        log.info("Uploading {} to Cloudinary as resource_type={}", assetType, resourceType);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", resourceType,
                            "folder", folder,
                            "use_filename", true,
                            "unique_filename", true));

            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl == null || secureUrl.isBlank()) {
                throw new RuntimeException("Cloudinary không trả về URL sau khi upload!");
            }

            log.info("Upload thành công: {}", secureUrl);
            return secureUrl;
        } catch (RuntimeException e) {
            log.error("Upload thất bại: ", e);
            throw new RuntimeException("Upload thất bại: " + e.getMessage(), e);
        }
    }

    private String buildFolder(AssetType assetType) {
        return switch (assetType) {
            case POSTER -> "lovestream/movies/posters";
            case TRAILER -> "lovestream/movies/trailers";
            case FULL_VIDEO -> "lovestream/movies/videos";
            case SEASON_POSTER -> "lovestream/series/posters";
            case EPISODE_VIDEO -> "lovestream/series/videos";
            default -> "lovestream/media";
        };
    }
}
