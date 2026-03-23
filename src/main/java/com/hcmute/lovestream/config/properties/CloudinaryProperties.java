package com.hcmute.lovestream.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudinary")
@Getter
@Setter
public class CloudinaryProperties {
    private String cloudName;
    private String apiKey;
    private String apiSecret;
}
