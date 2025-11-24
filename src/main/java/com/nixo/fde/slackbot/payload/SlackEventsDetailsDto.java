package com.nixo.fde.slackbot.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackEventsDetailsDto {

    private String type;
    private String channel;
    private String user;
    private String text;

    @JsonProperty("ts")
    private String timestamp;

    @JsonProperty("event_ts")
    private String eventTimestamp;

    @JsonProperty("channel_type")
    private String channelType;

    @JsonProperty("thread_ts")
    private String threadTs;
}
