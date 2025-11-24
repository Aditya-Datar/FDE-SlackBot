package com.nixo.fde.slackbot.payload;

public class ClassificationResultDto {
    private final boolean relevant;
    private final String category;
    private final String title;
    private final double confidence;

    public ClassificationResultDto(boolean relevant, String category, String title, double confidence) {
        this.relevant = relevant;
        this.category = category;
        this.title = title;
        this.confidence = confidence;
    }

    public static ClassificationResultDto irrelevant() {
        return new ClassificationResultDto(false, "NONE", null, 0.0);
    }

    public boolean isRelevant() {
        return relevant;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public double getConfidence() {
        return confidence;
    }
}
