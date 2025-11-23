package com.nixo.fde.slackbot.controller;

import com.nixo.fde.slackbot.payload.SlackEventDto;
import com.nixo.fde.slackbot.service.SlackBotEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackBotController {
    private final SlackBotEventService slackBotEventService;

    @PostMapping("/events")
    public ResponseEntity<?> handleSlackEvent(
            @RequestBody SlackEventDto eventDto,
            @RequestHeader(value = "X-Slack-Signature", required = false) String signature,
            @RequestHeader(value = "X-Slack-Request-Timestamp", required = false) String timestamp
    ) {
        log.info("Received Slack event: type={}", eventDto.getType());

        // Handle URL verification challenge
        if ("url_verification".equals(eventDto.getType())) {
            log.info("Responding to URL verification challenge");
            return ResponseEntity.ok(Map.of("challenge", eventDto.getChallenge()));
        }

        // Handle event callback
        if ("event_callback".equals(eventDto.getType())) {
            // Ignore bot messages to prevent loops
            if (eventDto.getEvent().getBotId() != null) {
                log.debug("Ignoring bot message");
                return ResponseEntity.ok().build();
            }

            // Process the event asynchronously
            slackBotEventService.processEventAsync(eventDto);

            // Respond immediately to Slack (must respond within 3 seconds)
            return ResponseEntity.ok().build();
        }

        log.warn("Unknown event type: {}", eventDto.getType());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "FDE Slackbot"
        ));
    }
}
