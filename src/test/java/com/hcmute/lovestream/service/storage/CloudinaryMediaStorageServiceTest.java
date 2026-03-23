package com.hcmute.lovestream.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.hcmute.lovestream.config.properties.CloudinaryProperties;
import com.hcmute.lovestream.entity.enums.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CloudinaryMediaStorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private CloudinaryProperties cloudinaryProperties;

    @InjectMocks
    private CloudinaryMediaStorageService cloudinaryMediaStorageService;

    @BeforeEach
    void setUp() {
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
        lenient().when(cloudinaryProperties.getApiKey()).thenReturn("real-api-key");
    }

    @Test
    void upload_WithPlaceholderApiKey_ThrowsIllegalStateException() {
        when(cloudinaryProperties.getApiKey()).thenReturn("my-api-key-placeholder");
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());

        assertThatThrownBy(() -> cloudinaryMediaStorageService.upload(file, AssetType.POSTER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cloudinary chưa được cấu hình. Vui lòng cập nhật cấu hình API key để sử dụng tính năng upload.");
    }

    // --- Test 1: Upload image (POSTER) returns secure_url ---
    @Test
    void whenUploadPoster_thenReturnSecureUrl() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );

        String expectedUrl = "https://res.cloudinary.com/test/image/upload/poster.jpg";
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", expectedUrl));

        String result = cloudinaryMediaStorageService.upload(mockFile, AssetType.POSTER);

        assertThat(result).isEqualTo(expectedUrl);
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    // --- Test 2: Upload video (MOVIE_VIDEO) uses video resource_type ---
    @Test
    void whenUploadMovieVideo_thenReturnSecureUrl() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "movie.mp4", "video/mp4", "fake-video-bytes".getBytes()
        );

        String expectedUrl = "https://res.cloudinary.com/test/video/upload/movie.mp4";
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", expectedUrl));

        String result = cloudinaryMediaStorageService.upload(mockFile, AssetType.MOVIE_VIDEO);

        assertThat(result).isEqualTo(expectedUrl);
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    // --- Test 3: Empty file throws IllegalArgumentException ---
    @Test
    void whenUploadEmptyFile_thenThrowIllegalArgumentException() {
        MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> cloudinaryMediaStorageService.upload(emptyFile, AssetType.POSTER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File upload không được để trống");

        verifyNoInteractions(uploader);
    }

    // --- Test 4: Cloudinary SDK throws exception, wrapped in RuntimeException ---
    @Test
    void whenCloudinaryFails_thenThrowRuntimeException() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "bad.jpg", "image/jpeg", "data".getBytes()
        );

        when(uploader.upload(any(byte[].class), anyMap()))
                .thenThrow(new RuntimeException("Cloudinary connection error"));

        assertThatThrownBy(() -> cloudinaryMediaStorageService.upload(mockFile, AssetType.BACKGROUND))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Upload thất bại");
    }

    // --- Test 5: Cloudinary returns missing secure_url throws exception ---
    @Test
    void whenCloudinaryReturnsMissingUrl_thenThrowRuntimeException() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "poster.png", "image/png", "bytes".getBytes()
        );

        // simulate Cloudinary not returning secure_url
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of()); // Empty map — no secure_url

        assertThatThrownBy(() -> cloudinaryMediaStorageService.upload(mockFile, AssetType.POSTER))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cloudinary không trả về URL");
    }

    // --- Test 6: TRAILER asset type is treated as video ---
    @Test
    void whenUploadTrailer_thenReturnUrl() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "trailer.mp4", "video/mp4", "trailer-bytes".getBytes()
        );

        String expectedUrl = "https://res.cloudinary.com/test/video/upload/trailer.mp4";
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", expectedUrl));

        String result = cloudinaryMediaStorageService.upload(mockFile, AssetType.TRAILER);

        assertThat(result).isEqualTo(expectedUrl);
    }
}
