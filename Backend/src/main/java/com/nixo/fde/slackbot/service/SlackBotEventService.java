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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackBotEventService {
    private final AIServiceInterface aiService;
    private final MessageGroupingService groupingService;
    private final SlackMessageRepository messageRepository;
    private final WebSocketNotificationService notificationService;

    @Async
    public void processEventAsync(SlackEventDto eventDto) {
        try {
            SlackEventsDetailsDto event = eventDto.getEvent();

            // 1. Fast Deduplication (Database Index Check)
            if (messageRepository.existsBySlackTimestamp(event.getTimestamp())) {
                return;
            }

            if(event.getText() == null){
                return;
            }

            ClassificationResultDto classification = null;
            String threadTs = event.getThreadTs();

            // --- OPTIMIZATION 1: THREAD CONTEXT CHECK (Skip Classification AI) ---
            if (threadTs != null && !threadTs.isEmpty()) {
                // Check if this thread already belongs to a ticket
                List<SlackMessage> parentMessages = messageRepository.findBySlackTimestampWithTicket(threadTs);

                if (!parentMessages.isEmpty()) {
                    SlackTicket existingTicket = parentMessages.get(0).getTicket();
                    log.info("Optimization: Inheriting context from Ticket {}. Skipping Classification AI.", existingTicket.getId());

                    // Manually construct 'Relevant' result
                    classification = new ClassificationResultDto(
                            true,                           // Force Relevant
                            existingTicket.getCategory(),   // Inherit Category
                            existingTicket.getTitle(),      // Inherit Title
                            1.0                             // Max Confidence
                    );
                }
            }
            // ---------------------------------------------------------------------

            // 2. Classify (Only if we didn't find a thread match above)
            if (classification == null) {
                String normalizedText = ApplicationUtils.normalizeText(event.getText());
                classification = aiService.classifyMessage(normalizedText);
            }

            if (!classification.isRelevant()) {
                log.info("Irrelevant Message: {}", event.getText());
                return;
            }

            // 3. Find or Create Ticket
            // We pass the whole aiService so groupingService can use it lazily if needed
            SlackTicket ticket = groupingService.processMessage(
                    event.getText(),
                    event.getThreadTs(),
                    classification,
                    aiService,
                    event.getUser(),
                    event.getChannel(),
                    event.getChannelType(),
                    event.getTimestamp()
            );

            // 4. Notify Frontend
            if (ticket != null) {
                // Determine if it's new or updated based on message count
                int messageCount = ticket.getMessages().size();
                SlackTicketDto ticketDto = SlackTicketDto.fromEntity(ticket);

                if (messageCount <= 1) {
                    notificationService.notifyTicketCreated(ticketDto);
                } else {
                    notificationService.notifyTicketUpdated(ticketDto);
                }
            } else {
                log.info("Silent update (duplicate content). No notification sent.");
            }

        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
        }
    }
}