package com.hcmute.lovestream.service.storage.impl;

import com.hcmute.lovestream.service.storage.WebContentLocalStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class WebContentLocalStorageServiceImpl implements WebContentLocalStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L; // 5MB

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public String storeImage(MultipartFile file, String subFolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload không được để trống.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Dung lượng file tối đa là 5MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("File upload không có tên hợp lệ.");
        }

        String ext = extractExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ: jpg, jpeg, png, webp.");
        }

        String safeSubFolder = sanitizeSubFolder(subFolder);
        Path rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetDir = rootPath.resolve(safeSubFolder);
        Files.createDirectories(targetDir);

        String filename = UUID.randomUUID() + "." + ext;
        Path targetPath = targetDir.resolve(filename);
        file.transferTo(targetPath);

        // Use forward slash for URL paths.
        return safeSubFolder + "/" + filename;
    }

    private String sanitizeSubFolder(String subFolder) {
        String value = subFolder == null ? "" : subFolder.trim();
        if (value.isBlank()) {
            return "";
        }
        value = value.replace("\\", "/");
        while (value.startsWith("/")) value = value.substring(1);
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        if (value.contains("..")) {
            throw new IllegalArgumentException("Thư mục upload không hợp lệ.");
        }
        return value;
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1);
    }
}

