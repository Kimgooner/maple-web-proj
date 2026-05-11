package org.whitedoggy.maplehistory.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexon.api")
public record NexonApiProperties(
        String baseUrl,
        String apiKey,
        Duration requestTimeout,
        int maxConcurrency
) {
    public NexonApiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://open.api.nexon.com";
        }
        if (requestTimeout == null) {
            requestTimeout = Duration.ofSeconds(5);
        }
        if (maxConcurrency <= 0) {
            maxConcurrency = 8;
        }
    }
}
