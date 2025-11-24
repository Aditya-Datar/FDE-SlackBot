package com.nixo.fde.slackbot.service;

import com.nixo.fde.slackbot.payload.ClassificationResultDto;

import java.util.List;

public interface AIServiceInterface {
    /**
     * Classify a Slack message to determine relevance and category
     */
    ClassificationResultDto classifyMessage(String messageText);

    /**
     * Generate embedding vector for semantic similarity
     */
    List<Double> generateEmbedding(String text);
}
