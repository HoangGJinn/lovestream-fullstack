package com.hcmute.lovestream.controller.web.contentmanager;

import com.hcmute.lovestream.service.storage.WebContentLocalStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/content-manager/web-content/editor")
@RequiredArgsConstructor
@Slf4j
public class ContentManagerWebContentEditorController {

    private final WebContentLocalStorageService localStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String relativePath = localStorageService.storeImage(file, "editor");
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("url", "/uploads/" + relativePath);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (IOException e) {
            log.error("Editor image upload failed", e);
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", "Lưu ảnh thất bại: " + e.getMessage());
            return ResponseEntity.internalServerError().body(body);
        }
    }
}

