package com.nixo.fde.slackbot.service;

import com.google.gson.Gson;
import com.nixo.fde.slackbot.models.SlackMessage;
import com.nixo.fde.slackbot.models.SlackTicket;
import com.nixo.fde.slackbot.payload.ClassificationResultDto;
import com.nixo.fde.slackbot.payload.SlackEventDto;
import com.nixo.fde.slackbot.payload.SlackEventsDetailsDto;
import com.nixo.fde.slackbot.payload.SlackTicketDto;
import com.nixo.fde.slackbot.repository.SlackMessageRepository;
import com.nixo.fde.slackbot.utils.ApplicationUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackBotEventService {
    private final AIServiceInterface aiService;
    private final MessageGroupingService groupingService;
    private final SlackMessageRepository messageRepository;
    private final Gson gson = new Gson();

    @Async
    @Transactional
    public void processEventAsync(SlackEventDto eventDto) {
        try {
            log.info("Processing event asynchronously: eventId={}", eventDto.getEventId());

            SlackEventsDetailsDto event = eventDto.getEvent();

            // Check for duplicate
            if (messageRepository.existsBySlackTimestamp(event.getTimestamp())) {
                log.info("Message already processed: {}", event.getTimestamp());
                return;
            }

            log.info("Message received - User: {}, Channel: {}, Text: {}",
                    event.getUser(),
                    event.getChannel(),
                    event.getText()
            );

            // Step 1: Classify message with AI service (Gemini or OpenAI)
            ClassificationResultDto classification =
                    aiService.classifyMessage(event.getText());

            log.info("Classification result - Relevant: {}, Category: {}, Confidence: {}",
                    classification.isRelevant(),
                    classification.getCategory(),
                    classification.getConfidence()
            );

            // Step 2: If irrelevant, ignore
            if (!classification.isRelevant()) {
                log.info("Message classified as irrelevant, skipping");
                return;
            }

            // Step 3: Generate embedding for grouping
            List<Double> embedding = aiService.generateEmbedding(event.getText());
            log.info("Generated embedding with {} dimensions", embedding.size());

            // Step 4: Find or create ticket (grouping logic)
            SlackTicket ticket = groupingService.findOrCreateTicket(
                    event.getText(),
                    event.getThreadTs(),
                    classification.getCategory(),
                    classification.getTitle(),
                    embedding
            );

            // Step 5: Create and save message
            SlackMessage message = SlackMessage.builder()
                    .ticket(ticket)
                    .slackText(event.getText())
                    .slackUser(event.getUser())
                    .channel(event.getChannel())
                    .slackTimestamp(event.getTimestamp())
                    .threadTs(event.getThreadTs())
                    .channelType(event.getChannelType())
                    .embedding(gson.toJson(embedding))
                    .slackMessageTime(parseSlackTimestamp(event.getTimestamp()))
                    .createdAt(ApplicationUtils.getCurrentUtcDateTime())
                    .build();

            messageRepository.save(message);

            log.info("Message saved successfully to ticket: {}", ticket.getId());

            // TODO: Task 5 - Send WebSocket notification to frontend

        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
        }
    }

    private LocalDateTime parseSlackTimestamp(String slackTs) {
        try {
            String[] parts = slackTs.split("\\.");
            long seconds = Long.parseLong(parts[0]);
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Error parsing slack timestamp: {}, using current time", slackTs);
            return LocalDateTime.now();
        }
    }
}
