package com.hcmute.lovestream.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class VietnameseNormalizer {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");

    private VietnameseNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        String replaced = trimmed
                .replace('đ', 'd')
                .replace('Đ', 'D');

        String normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");

        return withoutDiacritics
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}

