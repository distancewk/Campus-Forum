package com.campus.ai.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "campus.ai")
public class AiProperties {
    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String chatModel;
    private String embeddingModel;
    private int embeddingDimension = 1536;
    private int timeoutMs = 15000;
    private Qa qa = new Qa();
    private Moderation moderation = new Moderation();
    private Document document = new Document();

    @PostConstruct
    public void validate() {
        if (embeddingDimension != 1536) {
            throw new IllegalStateException("当前数据库向量列固定为 1536 维，请将 campus.ai.embedding-dimension 配置为 1536");
        }
    }

    @Data
    public static class Qa {
        private int maxSources = 6;
        private double minScore = 0.32;
        private int rateLimitPerHour = 30;
    }

    @Data
    public static class Moderation {
        private boolean enabled = true;
        private double mediumThreshold = 0.55;
        private double highThreshold = 0.82;
    }

    @Data
    public static class Document {
        private long maxFileSize = 10 * 1024 * 1024;
    }
}
