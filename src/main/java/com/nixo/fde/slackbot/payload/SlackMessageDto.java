package com.nixo.fde.slackbot.payload;

import com.nixo.fde.slackbot.models.SlackMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlackMessageDto {
    private Long id;
    private Long ticketId;
    private String text;
    private String user;
    private String channel;
    private String slackTimestamp;
    private String threadTs;
    private String channelType;
    private LocalDateTime createdAt;
    private LocalDateTime slackMessageTime;

    public static SlackMessageDto fromEntity(SlackMessage message) {
        return SlackMessageDto.builder()
                .id(message.getId())
                .ticketId(message.getTicket().getId())
                .text(message.getText())
                .user(message.getUser())
                .channel(message.getChannel())
                .slackTimestamp(message.getSlackTimestamp())
                .threadTs(message.getThreadTs())
                .channelType(message.getChannelType())
                .createdAt(message.getCreatedAt())
                .slackMessageTime(message.getSlackMessageTime())
                .build();
    }
}
