package com.hcmute.lovestream.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hcmute.lovestream.config.properties.CloudinaryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryMediaStorageService implements MediaStorageService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    @Override
    public String upload(MultipartFile file, CloudinaryFolderTarget folderTarget) throws IOException {
        String apiKey = cloudinaryProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("placeholder")) {
            throw new IllegalStateException(
                    "Cloudinary chưa được cấu hình. Vui lòng cập nhật cấu hình API key để sử dụng tính năng upload.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload không được để trống!");
        }

        log.info("Uploading to Cloudinary folder={} as resource_type={}",
                folderTarget.getFolderPath(), folderTarget.getResourceType());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", folderTarget.getResourceType(),
                            // Use asset_folder so uploads land in the Media Library folder tree for dynamic-folder environments.
                            "asset_folder", folderTarget.getFolderPath(),
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
}
