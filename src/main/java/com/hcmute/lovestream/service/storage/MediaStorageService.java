package com.hcmute.lovestream.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Abstraction for cloud media storage.
 * Swap implementations (Cloudinary, S3, Firebase, etc.) without touching business code.
 */
public interface MediaStorageService {

    /**
     * Upload a media file to remote storage.
     *
     * @param file         the file to upload
     * @param folderTarget the Cloudinary folder target to upload into
     * @return the public URL of the uploaded file
     * @throws IOException if the upload fails
     */
    String upload(MultipartFile file, CloudinaryFolderTarget folderTarget) throws IOException;
}
