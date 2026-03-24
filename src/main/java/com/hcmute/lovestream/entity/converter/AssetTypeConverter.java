package com.hcmute.lovestream.entity.converter;

import com.hcmute.lovestream.entity.enums.AssetType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AssetTypeConverter implements AttributeConverter<AssetType, String> {

    @Override
    public String convertToDatabaseColumn(AssetType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public AssetType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return switch (dbData) {
            case "MOVIE_VIDEO" -> AssetType.FULL_VIDEO;
            case "BACKGROUND" -> AssetType.POSTER;
            default -> {
                try {
                    yield AssetType.valueOf(dbData);
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
}
