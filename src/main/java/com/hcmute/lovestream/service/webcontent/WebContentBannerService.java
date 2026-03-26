package com.hcmute.lovestream.service.webcontent;

import com.hcmute.lovestream.dto.request.webcontent.WebContentBannerReorderRequest;
import com.hcmute.lovestream.dto.request.webcontent.WebContentBannerUpsertRequest;
import com.hcmute.lovestream.entity.WebContentBanner;

import java.util.List;

public interface WebContentBannerService {
    List<WebContentBanner> getAllOrdered();

    List<WebContentBanner> getDisplayedForHome();

    WebContentBanner getOrThrow(Long bannerId);

    WebContentBanner create(WebContentBannerUpsertRequest request);

    WebContentBanner update(Long bannerId, WebContentBannerUpsertRequest request);

    void toggleDisplayed(Long bannerId);

    void delete(Long bannerId);

    void reorder(WebContentBannerReorderRequest request);
}

