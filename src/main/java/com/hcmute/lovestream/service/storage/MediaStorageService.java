package com.hcmute.lovestream.service.storage;

import com.hcmute.lovestream.entity.enums.AssetType;
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
     * @param file      the file to upload
     * @param assetType the type of asset (POSTER, TRAILER, MOVIE_VIDEO, etc.)
     * @return the public URL of the uploaded file
     * @throws IOException if the upload fails
     */
    String upload(MultipartFile file, AssetType assetType) throws IOException;
}
