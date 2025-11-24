package com.nixo.fde.slackbot.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nixo.fde.slackbot.models.SlackMessage;
import com.nixo.fde.slackbot.models.SlackTicket;
import com.nixo.fde.slackbot.payload.ClassificationResultDto;
import com.nixo.fde.slackbot.repository.SlackMessageRepository;
import com.nixo.fde.slackbot.repository.SlackTicketRepository;
import com.nixo.fde.slackbot.utils.ApplicationUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageGroupingService {

    private final SlackTicketRepository ticketRepository;
    private final SlackMessageRepository messageRepository;
    private final Gson gson = new Gson();

    @Value("${grouping.time.window.hours:24}")
    private int timeWindowHours;

    // Caches
    private final Map<Long, List<Double>> vectorCache = new ConcurrentHashMap<>();
    private final Map<String, List<Double>> textEmbeddingCache = new ConcurrentHashMap<>();

    @Transactional
    public SlackTicket processMessage(
            String messageText,
            String threadTs,
            ClassificationResultDto classification,
            AIServiceInterface aiService,
            String user,
            String channel,
            String channelType,
            String slackTimestamp
    ) {
        SlackTicket ticket = null;
        List<Double> embedding = null;

        // ---------------------------------------------------------
        // 1. FAST PATH: Check Thread ID (Zero Latency)
        // ---------------------------------------------------------
        if (threadTs != null && !threadTs.isEmpty()) {
            List<SlackMessage> threadMessages = messageRepository.findBySlackTimestamp(threadTs);
            if (!threadMessages.isEmpty()) {
                log.info("Fast Match: Found existing ticket via thread_ts");
                ticket = threadMessages.get(0).getTicket();
            }
        }

        // ---------------------------------------------------------
        // 2. SLOW PATH: Vector Similarity (Only if not a thread)
        // ---------------------------------------------------------
        if (ticket == null) {
            // OPTIMIZATION: Use internal cache wrapper, not direct service call!
            embedding = getEmbeddingWithCache(messageText, aiService);

            if (embedding != null && !embedding.isEmpty()) {
                ticket = findSimilarTicket(embedding, classification.getCategory());
            }

            if (ticket == null) {
                log.info("No match found. Creating new ticket: {}", classification.getTitle());
                ticket = createNewTicket(classification.getCategory(), classification.getTitle());
            }
        }

        // ---------------------------------------------------------
        // 3. CONTENT DEDUPLICATION & HIBERNATE FIX
        // ---------------------------------------------------------

        // Fix for LazyInitializationException
        if (ticket.getMessages() == null) {
            ticket.setMessages(new ArrayList<>());
        }
        ticket.getMessages().size(); // Wakes up Hibernate

        // Check for duplicates
        boolean isDuplicateContent = ticket.getMessages().stream()
                .anyMatch(msg -> msg.getSlackText().trim().equalsIgnoreCase(messageText.trim()));

        if (isDuplicateContent) {
            log.info("Duplicate content detected in Ticket {}. Updating timestamp silently.", ticket.getId());
            ticket.setUpdatedAt(ApplicationUtils.getCurrentUtcDateTime());
            ticketRepository.save(ticket);

            // Return NULL to signal EventService to stay silent
            return null;
        }

        // ---------------------------------------------------------
        // 4. SAVE
        // ---------------------------------------------------------
        SlackMessage savedMsg = saveMessage(ticket, messageText, user, channel, channelType, slackTimestamp, threadTs, embedding);

        // Add to list so UI sees it
        ticket.getMessages().add(savedMsg);

        return ticket;
    }

    // ================= HELPER METHODS =================

    private SlackMessage saveMessage(SlackTicket ticket, String text, String user, String channel, String cType, String ts, String threadTs, List<Double> embedding) {
        SlackMessage message = SlackMessage.builder()
                .ticket(ticket)
                .slackText(text)
                .slackUser(user)
                .channel(channel)
                .channelType(cType)
                .slackTimestamp(ts)
                .threadTs(threadTs)
                .embedding(embedding != null ? gson.toJson(embedding) : null)
                .slackMessageTime(ApplicationUtils.parseSlackTimestamp(ts))
                .createdAt(ApplicationUtils.getCurrentUtcDateTime())
                .build();

        SlackMessage saved = messageRepository.save(message);

        ticket.setUpdatedAt(ApplicationUtils.getCurrentUtcDateTime());
        ticketRepository.save(ticket);

        return saved;
    }

    private SlackTicket createNewTicket(String category, String title) {
        SlackTicket ticket = SlackTicket.builder()
                .title(title)
                .category(category)
                .status("OPEN")
                .updatedAt(ApplicationUtils.getCurrentUtcDateTime())
                .createdAt(ApplicationUtils.getCurrentUtcDateTime())
                .build();
        return ticketRepository.save(ticket);
    }

    private SlackTicket findSimilarTicket(List<Double> targetEmbedding, String category) {
        LocalDateTime since = LocalDateTime.now().minusHours(timeWindowHours);
        List<SlackMessage> recentMessages = messageRepository.findRecentMessagesWithEmbeddings(since);

        double maxSimilarity = -1.0;
        SlackTicket bestMatch = null;

        for (SlackMessage msg : recentMessages) {
            if (!msg.getTicket().getCategory().equalsIgnoreCase(category)) continue;

            List<Double> candidateVector = vectorCache.computeIfAbsent(msg.getTicket().getId(), k ->
                    parseEmbedding(msg.getEmbedding())
            );

            if (candidateVector.isEmpty()) continue;

            double similarity = cosineSimilarity(targetEmbedding, candidateVector);

            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestMatch = msg.getTicket();
            }
        }

        if (maxSimilarity >= ApplicationUtils.getSimilarityThreshold(category)) {
            return bestMatch;
        }
        return null;
    }

    // Adding Caching to Embedding to Reduce API calls
    private List<Double> getEmbeddingWithCache(String text, AIServiceInterface aiService) {
        // 1. Exact Match
        if (textEmbeddingCache.containsKey(text)) return textEmbeddingCache.get(text);

        // 2. Fuzzy Match (Levenshtein)
        for (Map.Entry<String, List<Double>> entry : textEmbeddingCache.entrySet()) {
            if (Math.abs(entry.getKey().length() - text.length()) > 10) continue;
            if (ApplicationUtils.calculateStringSimilarity(text, entry.getKey()) >= 0.90) {
                textEmbeddingCache.put(text, entry.getValue());
                return entry.getValue();
            }
        }

        // 3. AI Service
        List<Double> embedding = aiService.generateEmbedding(text);
        if (embedding != null && !embedding.isEmpty()) {
            textEmbeddingCache.put(text, embedding);
        }
        return embedding;
    }

    private List<Double> parseEmbedding(String embeddingJson) {
        try {
            return gson.fromJson(embeddingJson, new TypeToken<List<Double>>(){}.getType());
        } catch (Exception e) { return List.of(); }
    }

    private double cosineSimilarity(List<Double> vec1, List<Double> vec2) {
        if (vec1.size() != vec2.size()) return 0.0;
        double dotProduct = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }
        return (norm1 == 0 || norm2 == 0) ? 0.0 : dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

}