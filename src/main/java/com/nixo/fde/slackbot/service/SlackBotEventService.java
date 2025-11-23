package com.nixo.fde.slackbot.service;

import com.nixo.fde.slackbot.payload.SlackEventDto;
import com.nixo.fde.slackbot.payload.SlackEventsDetailsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackBotEventService {

    @Async
    public void processEventAsync(SlackEventDto eventDto) {
        try {
            log.info("Processing event asynchronously: eventId={}", eventDto.getEventId());

            SlackEventsDetailsDto event = eventDto.getEvent();

            // Log event details
            log.info("Message received - User: {}, Channel: {}, Text: {}, Thread: {}",
                    event.getUser(),
                    event.getChannel(),
                    event.getText(),
                    event.getThreadTs()
            );

            log.info("Event processed successfully");

        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
        }
    }
}
