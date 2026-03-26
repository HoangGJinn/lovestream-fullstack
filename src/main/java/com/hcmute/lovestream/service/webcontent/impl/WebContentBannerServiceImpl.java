package com.hcmute.lovestream.service.webcontent.impl;

import com.hcmute.lovestream.dto.request.webcontent.WebContentBannerReorderRequest;
import com.hcmute.lovestream.dto.request.webcontent.WebContentBannerUpsertRequest;
import com.hcmute.lovestream.entity.WebContentBanner;
import com.hcmute.lovestream.repository.WebContentBannerRepository;
import com.hcmute.lovestream.service.storage.WebContentLocalStorageService;
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

    @Override
    public List<WebContentBanner> getAllOrdered() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    public List<WebContentBanner> getDisplayedForHome() {
        return bannerRepository.findByIsDisplayedTrueOrderByDisplayOrderAsc();
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
                .navigationLink(trimToNull(request.getNavigationLink()))
                .displayOrder(request.getDisplayOrder())
                .isDisplayed(Boolean.TRUE.equals(request.getIsDisplayed()))
                .imagePath(imagePath)
                .build();

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
        existing.setNavigationLink(trimToNull(request.getNavigationLink()));
        existing.setDisplayOrder(request.getDisplayOrder());
        existing.setIsDisplayed(Boolean.TRUE.equals(request.getIsDisplayed()));

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

        // Unique ids + validate displayOrder.
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
        if (requireImage) {
            if (request.getBannerImage() == null || request.getBannerImage().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn ảnh banner.");
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}

