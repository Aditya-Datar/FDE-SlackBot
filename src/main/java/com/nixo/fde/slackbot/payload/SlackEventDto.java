package com.nixo.fde.slackbot.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackEventDto {
    private String token;

    private String teamId;

    private String apiAppId;

    private String type;

    private String eventId;

    private Long eventTime;

    private SlackEventsDetailsDto event;

    private String challenge;
}
