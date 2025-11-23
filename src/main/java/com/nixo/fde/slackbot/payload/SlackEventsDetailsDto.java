package com.nixo.fde.slackbot.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackEventsDetailsDto {
    private String type;

    private String user;

    private String text;

    private String timestamp;

    private String channel;

    private String eventTimestamp;

    private String channelType;

    private String threadTs;

    private String botId;
}
