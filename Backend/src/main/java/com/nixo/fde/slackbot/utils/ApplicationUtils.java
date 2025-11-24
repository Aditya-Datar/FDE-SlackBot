package com.nixo.fde.slackbot.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

@Slf4j
public class ApplicationUtils {
    public static LocalDateTime getCurrentUtcDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    // In GroupingService.java
    public static double getSimilarityThreshold(String category) {
        return switch (category) {
            case "BUG" -> 0.82;              // More lenient for bugs
            case "FEATURE_REQUEST" -> 0.85;  // Stricter for features
            case "SUPPORT" -> 0.80;          // More lenient for support
            default -> 0.83;
        };
    }

}
