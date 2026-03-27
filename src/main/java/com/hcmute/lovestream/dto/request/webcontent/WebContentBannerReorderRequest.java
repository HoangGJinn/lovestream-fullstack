package com.hcmute.lovestream.dto.request.webcontent;

import java.util.List;

public record WebContentBannerReorderRequest(List<WebContentBannerOrderItem> orders) {

    public record WebContentBannerOrderItem(Long id, Integer displayOrder) {
    }
}

