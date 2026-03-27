package com.hcmute.lovestream.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.exceptions.AlreadyExists;
import com.cloudinary.utils.ObjectUtils;
import com.hcmute.lovestream.config.properties.CloudinaryProperties;
import com.hcmute.lovestream.service.storage.CloudinaryFolderTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

//@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryFolderInitializer {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureFolderTreeExists() {
        if (!isConfigured()) {
            log.info("Skip Cloudinary folder initialization because Cloudinary is not configured.");
            return;
        }

        for (String folderPath : CloudinaryFolderTarget.folderCreationOrder()) {
            try {
                cloudinary.api().createFolder(folderPath, ObjectUtils.emptyMap());
                log.info("Created Cloudinary folder: {}", folderPath);
            } catch (AlreadyExists ignored) {
                log.debug("Cloudinary folder already exists: {}", folderPath);
            } catch (Exception ex) {
                log.warn("Could not ensure Cloudinary folder exists: {}", folderPath, ex);
            }
        }
    }

    private boolean isConfigured() {
        String cloudName = cloudinaryProperties.getCloudName();
        String apiKey = cloudinaryProperties.getApiKey();
        String apiSecret = cloudinaryProperties.getApiSecret();

        return isPresent(cloudName) && isPresent(apiKey) && isPresent(apiSecret)
                && !apiKey.contains("placeholder");
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
