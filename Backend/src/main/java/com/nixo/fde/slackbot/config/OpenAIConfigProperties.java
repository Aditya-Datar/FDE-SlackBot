package com.nixo.fde.slackbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAIConfigProperties {

    private Api api;
    private String model;
    private Embedding embedding;
    private Max max;
    private Double temperature;

    @Data
    public static class Api {
        private String key;
    }

    @Data
    public static class Embedding {
        private String model;
    }

    @Data
    public static class Max {
        private Integer tokens;
    }
}