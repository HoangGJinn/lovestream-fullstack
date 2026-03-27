package com.hcmute.lovestream.service.webcontent.impl;

import com.hcmute.lovestream.dto.request.webcontent.WebContentBannerReorderRequest;
import com.hcmute.lovestream.dto.request.webcontent.WebContentBannerUpsertRequest;
import com.hcmute.lovestream.entity.WebContentBanner;
import com.hcmute.lovestream.entity.enums.WebContentBannerTargetType;
import com.hcmute.lovestream.entity.enums.WebStaticPageType;
import com.hcmute.lovestream.repository.MovieRepository;
import com.hcmute.lovestream.repository.StaticPageRepository;
import com.hcmute.lovestream.repository.TVSeriesRepository;
import com.hcmute.lovestream.repository.WebContentBannerRepository;
import com.hcmute.lovestream.service.storage.WebContentLocalStorageService;
import com.hcmute.lovestream.service.webcontent.WebContentBannerNavigationResolver;
import com.hcmute.lovestream.service.webcontent.WebContentBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebContentBannerServiceImpl implements WebContentBannerService {

    private final WebContentBannerRepository bannerRepository;
    private final WebContentLocalStorageService localStorageService;
    private final WebContentBannerNavigationResolver navigationResolver;
    private final MovieRepository movieRepository;
    private final TVSeriesRepository tvSeriesRepository;
    private final StaticPageRepository staticPageRepository;

    @Override
    public List<WebContentBanner> getAllOrdered() {
        List<WebContentBanner> banners = bannerRepository.findAllByOrderByDisplayOrderAsc();
        navigationResolver.populateResolvedFields(banners);
        return banners;
    }

    @Override
    public List<WebContentBanner> getDisplayedForHome() {
        List<WebContentBanner> banners = bannerRepository.findByIsDisplayedTrueOrderByDisplayOrderAsc();
        navigationResolver.populateResolvedFields(banners);
        return banners;
    }

    @Override
    public WebContentBanner getOrThrow(Long bannerId) {
        if (bannerId == null) {
            throw new IllegalArgumentException("ID banner không hợp lệ.");
        }
        return bannerRepository.findById(bannerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy banner."));
    }

    @Override
    @Transactional
    public WebContentBanner create(WebContentBannerUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ.");
        }
        validateUpsertRequest(request, true);

        MultipartFile bannerImage = request.getBannerImage();
        String imagePath;
        try {
            imagePath = localStorageService.storeImage(bannerImage, "banners");
        } catch (IOException e) {
            throw new RuntimeException("Lưu ảnh banner thất bại: " + e.getMessage(), e);
        }

        WebContentBanner banner = WebContentBanner.builder()
                .title(request.getTitle().trim())
                .displayOrder(request.getDisplayOrder())
                .isDisplayed(Boolean.TRUE.equals(request.getIsDisplayed()))
                .imagePath(imagePath)
                .build();
        applyNavigationTarget(banner, request);

        return java.util.Objects.requireNonNull(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public WebContentBanner update(Long bannerId, WebContentBannerUpsertRequest request) {
        if (bannerId == null) {
            throw new IllegalArgumentException("ID banner không hợp lệ.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ.");
        }

        WebContentBanner existing = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy banner để cập nhật."));

        validateUpsertRequest(request, false);

        existing.setTitle(request.getTitle().trim());
        existing.setDisplayOrder(request.getDisplayOrder());
        existing.setIsDisplayed(Boolean.TRUE.equals(request.getIsDisplayed()));
        applyNavigationTarget(existing, request);

        MultipartFile newImage = request.getBannerImage();
        if (newImage != null && !newImage.isEmpty()) {
            try {
                String newImagePath = localStorageService.storeImage(newImage, "banners");
                existing.setImagePath(newImagePath);
            } catch (IOException e) {
                throw new RuntimeException("Lưu ảnh banner thất bại: " + e.getMessage(), e);
            }
        }

        return bannerRepository.save(existing);
    }

    @Override
    @Transactional
    public void toggleDisplayed(Long bannerId) {
        WebContentBanner banner = getOrThrow(bannerId);
        banner.setIsDisplayed(!Boolean.TRUE.equals(banner.getIsDisplayed()));
        bannerRepository.save(banner);
    }

    @Override
    @Transactional
    public void delete(Long bannerId) {
        if (bannerId == null) {
            throw new IllegalArgumentException("ID banner không hợp lệ.");
        }
        if (!bannerRepository.existsById(bannerId)) {
            throw new IllegalArgumentException("Không tìm thấy banner để xóa.");
        }
        bannerRepository.deleteById(bannerId);
    }

    @Override
    @Transactional
    public void reorder(WebContentBannerReorderRequest request) {
        if (request == null || request.orders() == null) {
            throw new IllegalArgumentException("Yêu cầu sắp xếp không hợp lệ.");
        }
        if (request.orders().isEmpty()) {
            return;
        }

        Set<Long> seen = new HashSet<>();
        for (WebContentBannerReorderRequest.WebContentBannerOrderItem item : request.orders()) {
            if (item == null || item.id() == null) {
                throw new IllegalArgumentException("Dữ liệu sắp xếp không hợp lệ.");
            }
            if (!seen.add(item.id())) {
                throw new IllegalArgumentException("Danh sách sắp xếp chứa ID bị trùng: " + item.id());
            }
            if (item.displayOrder() == null) {
                throw new IllegalArgumentException("Thứ tự hiển thị không được để trống.");
            }
        }

        List<WebContentBanner> toSave = new ArrayList<>();
        for (WebContentBannerReorderRequest.WebContentBannerOrderItem item : request.orders()) {
            Long id = java.util.Objects.requireNonNull(item.id());
            WebContentBanner banner = bannerRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy banner: " + id));
            banner.setDisplayOrder(item.displayOrder());
            toSave.add(banner);
        }
        bannerRepository.saveAll(toSave);
    }

    private void validateUpsertRequest(WebContentBannerUpsertRequest request, boolean requireImage) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tiêu đề banner không được để trống.");
        }
        if (request.getDisplayOrder() == null) {
            throw new IllegalArgumentException("Thứ tự hiển thị không được để trống.");
        }
        if (request.getIsDisplayed() == null) {
            request.setIsDisplayed(true);
        }
        if (request.getTargetType() == null) {
            request.setTargetType(WebContentBannerTargetType.NONE);
        }

        String movieTargetId = trimToNull(request.getMovieTargetId());
        String seriesTargetId = trimToNull(request.getSeriesTargetId());
        String externalUrl = trimToNull(request.getExternalUrl());

        request.setMovieTargetId(movieTargetId);
        request.setSeriesTargetId(seriesTargetId);
        request.setExternalUrl(externalUrl);

        switch (request.getTargetType()) {
            case NONE -> {
                request.setMovieTargetId(null);
                request.setSeriesTargetId(null);
                request.setStaticPageTarget(null);
                request.setExternalUrl(null);
            }
            case MOVIE -> {
                request.setSeriesTargetId(null);
                request.setStaticPageTarget(null);
                request.setExternalUrl(null);
                if (movieTargetId == null) {
                    throw new IllegalArgumentException("Vui lòng chọn phim đích cho banner.");
                }
                if (!movieRepository.existsById(movieTargetId)) {
                    throw new IllegalArgumentException("Phim đích không còn tồn tại.");
                }
            }
            case SERIES -> {
                request.setMovieTargetId(null);
                request.setStaticPageTarget(null);
                request.setExternalUrl(null);
                if (seriesTargetId == null) {
                    throw new IllegalArgumentException("Vui lòng chọn series đích cho banner.");
                }
                if (!tvSeriesRepository.existsById(seriesTargetId)) {
                    throw new IllegalArgumentException("Series đích không còn tồn tại.");
                }
            }
            case STATIC_PAGE -> {
                request.setMovieTargetId(null);
                request.setSeriesTargetId(null);
                request.setExternalUrl(null);
                if (request.getStaticPageTarget() == null) {
                    throw new IllegalArgumentException("Vui lòng chọn trang tĩnh đích cho banner.");
                }
                if (!staticPageRepository.existsByPageType(request.getStaticPageTarget())) {
                    throw new IllegalArgumentException("Trang tĩnh đích chưa có nội dung.");
                }
            }
            case EXTERNAL_URL -> {
                request.setMovieTargetId(null);
                request.setSeriesTargetId(null);
                request.setStaticPageTarget(null);
                if (externalUrl == null) {
                    throw new IllegalArgumentException("Vui lòng nhập URL tùy chỉnh cho banner.");
                }
                if (!externalUrl.startsWith("/") && !externalUrl.startsWith("http://")
                        && !externalUrl.startsWith("https://")) {
                    throw new IllegalArgumentException(
                            "URL tùy chỉnh phải bắt đầu bằng '/', 'http://' hoặc 'https://'.");
                }
            }
        }

        if (requireImage && (request.getBannerImage() == null || request.getBannerImage().isEmpty())) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh banner.");
        }
    }

    private void applyNavigationTarget(WebContentBanner banner, WebContentBannerUpsertRequest request) {
        banner.setTargetType(request.getTargetType());
        banner.setNavigationLink(null);

        switch (request.getTargetType()) {
            case NONE -> {
                banner.setTargetRefId(null);
                banner.setExternalUrl(null);
            }
            case MOVIE -> {
                banner.setTargetRefId(request.getMovieTargetId());
                banner.setExternalUrl(null);
            }
            case SERIES -> {
                banner.setTargetRefId(request.getSeriesTargetId());
                banner.setExternalUrl(null);
            }
            case STATIC_PAGE -> {
                WebStaticPageType staticPageTarget = request.getStaticPageTarget();
                banner.setTargetRefId(staticPageTarget != null ? staticPageTarget.name() : null);
                banner.setExternalUrl(null);
            }
            case EXTERNAL_URL -> {
                banner.setTargetRefId(null);
                banner.setExternalUrl(request.getExternalUrl());
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
