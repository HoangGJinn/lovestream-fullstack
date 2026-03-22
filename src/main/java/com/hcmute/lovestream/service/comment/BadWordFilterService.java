package com.hcmute.lovestream.service.comment;

import java.util.Arrays;
import java.util.List;

public class BadWordFilterService {
    private static final List<String> BAD_WORDS = Arrays.asList("chửi_thề", "từ_bậy", "tục_tĩu", "spam_link");
    public boolean containsBadWord(String content) {
        String lower = content.toLowerCase();
        return BAD_WORDS.stream().anyMatch(lower::contains);
    }
}
