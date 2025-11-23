package com.nixo.fde.slackbot.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class SlackTicketStatusDto {
    private long totalTickets;
    private long openTickets;
}
