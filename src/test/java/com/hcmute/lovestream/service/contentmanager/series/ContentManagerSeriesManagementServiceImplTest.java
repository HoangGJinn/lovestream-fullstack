package com.hcmute.lovestream.service.contentmanager.series;

import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.TVSeries;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.repository.ContentCreditRepository;
import com.hcmute.lovestream.repository.EpisodeRepository;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.repository.MediaAssetRepository;
import com.hcmute.lovestream.repository.PersonRepository;
import com.hcmute.lovestream.repository.SeasonRepository;
import com.hcmute.lovestream.repository.TVSeriesRepository;
import com.hcmute.lovestream.repository.UserSeriesWatchStateRepository;
import com.hcmute.lovestream.repository.WatchHistoryRepository;
import com.hcmute.lovestream.service.contentmanager.series.impl.ContentManagerSeriesManagementServiceImpl;
import com.hcmute.lovestream.service.notification.SeriesReleaseNotificationService;
import com.hcmute.lovestream.service.storage.MediaStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentManagerSeriesManagementServiceImplTest {

    @Mock
    private TVSeriesRepository tvSeriesRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private WatchHistoryRepository watchHistoryRepository;

    @Mock
    private UserSeriesWatchStateRepository userSeriesWatchStateRepository;

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ContentCreditRepository contentCreditRepository;

    @Mock
    private MediaStorageService mediaStorageService;

    @Mock
    private SeriesReleaseNotificationService seriesReleaseNotificationService;

    @InjectMocks
    private ContentManagerSeriesManagementServiceImpl service;

    @Test
    void uploadSeriesPoster_shouldRejectUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "poster.gif",
                "image/gif",
                new byte[] {1, 2, 3}
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokeUploadSeriesPoster("series-1", file)
        );

        assertEquals("Poster chỉ hỗ trợ định dạng JPG, PNG hoặc WEBP.", exception.getMessage());
        verifyNoInteractions(mediaStorageService);
    }

    @Test
    void uploadSeriesPoster_shouldRejectOversizedFile() {
        byte[] content = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "poster.png",
                "image/png",
                content
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokeUploadSeriesPoster("series-1", file)
        );

        assertEquals("Poster không được vượt quá 5MB.", exception.getMessage());
        verifyNoInteractions(mediaStorageService);
    }

    @Test
    void uploadSeriesPoster_shouldPersistPosterAssetForValidFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "poster.webp",
                "image/webp",
                new byte[] {1, 2, 3}
        );

        TVSeries series = new TVSeries();
        series.setId("series-1");
        series.setMediaAssets(new ArrayList<>());

        when(tvSeriesRepository.findById("series-1")).thenReturn(Optional.of(series));
        when(mediaStorageService.upload(any(), any())).thenReturn("https://cdn.example.com/poster.webp");
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaAsset savedAsset = service.uploadSeriesPoster("series-1", file);

        ArgumentCaptor<MediaAsset> assetCaptor = ArgumentCaptor.forClass(MediaAsset.class);
        verify(mediaAssetRepository).save(assetCaptor.capture());

        MediaAsset persistedAsset = assetCaptor.getValue();
        assertNotNull(savedAsset);
        assertSame(series, persistedAsset.getVideoContent());
        assertEquals(AssetType.POSTER, persistedAsset.getAssetType());
        assertEquals("https://cdn.example.com/poster.webp", persistedAsset.getAssetUrl());
    }

    private void invokeUploadSeriesPoster(String seriesId, MockMultipartFile file) {
        try {
            service.uploadSeriesPoster(seriesId, file);
        } catch (IOException e) {
            throw new AssertionError("Did not expect IOException in validation test", e);
        }
    }
}
