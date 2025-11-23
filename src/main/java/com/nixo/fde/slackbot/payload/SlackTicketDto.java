package com.nixo.fde.slackbot.payload;

import com.nixo.fde.slackbot.models.SlackTicket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlackTicketDto {
    private Long id;
    private String title;
    private String category;
    private String status;
    private int messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SlackMessageDto> messages;

    public static SlackTicketDto fromEntity(SlackTicket ticket) {
        return SlackTicketDto.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .category(ticket.getCategory())
                .status(ticket.getStatus())
                .messageCount(ticket.getMessageCount())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    public static SlackTicketDto fromEntityWithMessages(SlackTicket ticket) {
        SlackTicketDto dto = fromEntity(ticket);
        dto.setMessages(
                ticket.getMessages().stream()
                        .map(SlackMessageDto::fromEntity)
                        .collect(Collectors.toList())
        );
        return dto;
    }
}
