package com.hcmute.lovestream.service.webcontent;

import com.hcmute.lovestream.entity.StaticPage;
import com.hcmute.lovestream.entity.enums.WebStaticPageType;

public interface WebContentStaticPageService {

    StaticPage getOrThrow(WebStaticPageType type);

    StaticPage updateContent(WebStaticPageType type, String htmlContent);
}

