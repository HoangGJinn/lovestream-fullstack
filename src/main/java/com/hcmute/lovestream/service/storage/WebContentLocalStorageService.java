package com.hcmute.lovestream.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Local-only upload for WEB CONTENT module (banner + editor images).
 * Do not use Cloudinary here.
 */
public interface WebContentLocalStorageService {
    /**
     * Store an image under configured upload root.
     *
     * @param file uploaded image
     * @param subFolder relative folder under upload dir (e.g. "banners", "editor")
     * @return relative image path (e.g. "banners/uuid.png") to be served as "/uploads/{path}"
     */
    String storeImage(MultipartFile file, String subFolder) throws IOException;
}

