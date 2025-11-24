package com.nixo.fde.slackbot.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
public class ApplicationUtils {
    public static LocalDateTime getCurrentUtcDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    // In GroupingService.java
    public static double getSimilarityThreshold(String category) {
        return switch (category) {
            case "BUG" -> 0.70;              // More lenient for bugs
            case "FEATURE_REQUEST" -> 0.85;  // Stricter for features
            case "SUPPORT" -> 0.80;          // More lenient for support
            default -> 0.83;
        };
    }

    public static LocalDateTime parseSlackTimestamp(String slackTs) {
        try {
            String[] parts = slackTs.split("\\.");
            long seconds = Long.parseLong(parts[0]);
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Error parsing slack timestamp: {}, using current time", slackTs);
            return LocalDateTime.now();
        }
    }

    /**
     * Calculates similarity (0.0 to 1.0) based on character edits.
     * "System Down" vs "System Down!" = 0.95
     */
    public static double calculateStringSimilarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) {
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) return 1.0;

        int editDistance = getLevenshteinDistance(longer, shorter);
        return (longerLength - editDistance) / (double) longerLength;
    }

    // Standard Levenshtein algorithm
    public static int getLevenshteinDistance(String s, String t) {
        int m = s.length(), n = t.length();
        int[][] d = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) d[i][0] = i;
        for (int j = 0; j <= n; j++) d[0][j] = j;

        for (int j = 1; j <= n; j++) {
            for (int i = 1; i <= m; i++) {
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    d[i][j] = Math.min(Math.min(
                                    d[i - 1][j] + 1,    // deletion
                                    d[i][j - 1] + 1),   // insertion
                            d[i - 1][j - 1] + 1 // substitution
                    );
                }
            }
        }
        return d[m][n];
    }

    public static String normalizeText(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() > 5000 ? trimmed.substring(0, 5000) : trimmed;
    }

}
