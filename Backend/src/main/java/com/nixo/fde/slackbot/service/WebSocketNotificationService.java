package com.nixo.fde.slackbot.service;

import com.nixo.fde.slackbot.payload.SlackTicketDto;
import com.nixo.fde.slackbot.payload.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    public void notifyTicketCreated(SlackTicketDto ticket) {
        try {
            log.info("Sending WebSocket notification: TICKET_CREATED for ticket {}", ticket.getId());

            WebSocketMessage message = WebSocketMessage.builder()
                    .type("TICKET_CREATED")
                    .data(ticket)
                    .build();

            messagingTemplate.convertAndSend("/topic/tickets", message);
            log.info("WebSocket notification sent successfully");
        } catch (Exception e) {
            log.error("Error sending WebSocket notification: {}", e.getMessage(), e);
        }
    }

    public void notifyTicketUpdated(SlackTicketDto ticket) {
        try {
            log.info("Sending WebSocket notification: TICKET_UPDATED for ticket {}", ticket.getId());

            WebSocketMessage message = WebSocketMessage.builder()
                    .type("TICKET_UPDATED")
                    .data(ticket)
                    .build();

            messagingTemplate.convertAndSend("/topic/tickets", message);
            log.info("WebSocket notification sent successfully");
        } catch (Exception e) {
            log.error("Error sending WebSocket notification: {}", e.getMessage(), e);
        }
    }
}
