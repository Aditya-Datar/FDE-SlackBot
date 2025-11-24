package com.nixo.fde.slackbot.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nixo.fde.slackbot.models.SlackMessage;
import com.nixo.fde.slackbot.models.SlackTicket;
import com.nixo.fde.slackbot.repository.SlackMessageRepository;
import com.nixo.fde.slackbot.repository.SlackTicketRepository;
import com.nixo.fde.slackbot.utils.ApplicationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageGroupingService {

    private final SlackTicketRepository ticketRepository;
    private final SlackMessageRepository messageRepository;
    private final Gson gson = new Gson();

    @Value("${grouping.time.window.hours:24}")
    private int timeWindowHours;

    public SlackTicket findOrCreateTicket(
            String messageText,
            String threadTs,
            String category,
            String title,
            List<Double> embedding
    ) {
        // Rule 1: If message is in a thread, check if we have a ticket for that thread
        if (threadTs != null && !threadTs.isEmpty()) {
            List<SlackMessage> threadMessages = messageRepository.findBySlackTimestamp(threadTs);
            if (!threadMessages.isEmpty()) {
                log.info("Found existing ticket via thread_ts: {}", threadTs);
                return threadMessages.get(0).getTicket();
            }
        }

        // Rule 2: Check for similar messages using embeddings
        if (embedding != null && !embedding.isEmpty()) {
            SlackTicket similarTicket = findSimilarTicket(embedding, category);
            if (similarTicket != null) {
                log.info("Found similar ticket via embedding similarity: {}", similarTicket.getId());
                return similarTicket;
            }
        }

        // Rule 3: Create new ticket
        log.info("Creating new ticket for message: {}", title);
        return createNewTicket(category, title);
    }

    private SlackTicket findSimilarTicket(List<Double> embedding, String category) {
        LocalDateTime since = LocalDateTime.now().minusHours(timeWindowHours);
        List<SlackMessage> recentMessages = messageRepository.findRecentMessagesWithEmbeddings(since);

        for (SlackMessage message : recentMessages) {
            if (message.getEmbedding() == null) continue;

            List<Double> existingEmbedding = parseEmbedding(message.getEmbedding());
            if (existingEmbedding.isEmpty()) continue;

            double similarity = cosineSimilarity(embedding, existingEmbedding);

            log.debug("Comparing with message {}: similarity = {}", message.getId(), similarity);

            SlackTicket ticket = message.getTicket();
            if (similarity >= ApplicationUtils.getSimilarityThreshold(ticket.getCategory())) {
                if (ticket.getCategory().equals(category)) {
                    log.info("Found similar message with similarity: {}", similarity);
                    return ticket;
                }
            }
        }

        return null;
    }

    private SlackTicket createNewTicket(String category, String title) {
        SlackTicket ticket = SlackTicket.builder()
                .title(title)
                .category(category)
                .status("OPEN")
                .build();
        return ticketRepository.save(ticket);
    }

    private List<Double> parseEmbedding(String embeddingJson) {
        try {
            return gson.fromJson(embeddingJson, new TypeToken<List<Double>>(){}.getType());
        } catch (Exception e) {
            log.error("Error parsing embedding: {}", e.getMessage());
            return List.of();
        }
    }

    private double cosineSimilarity(List<Double> vec1, List<Double> vec2) {
        if (vec1.size() != vec2.size()) {
            log.warn("Embedding size mismatch: {} vs {}", vec1.size(), vec2.size());
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}