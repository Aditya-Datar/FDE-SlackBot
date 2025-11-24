package com.nixo.fde.slackbot.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "slack_messages", indexes = {
        @Index(name = "idx_slack_ts", columnList = "slackTimestamp"),
        @Index(name = "idx_thread_ts", columnList = "threadTs"),
        @Index(name = "idx_channel", columnList = "channel")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlackMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SlackTicket ticket;

    @Column(nullable = false, length = 4000)
    private String slackText;

    @Column(nullable = false, length = 50)
    private String slackUser;

    @Column(nullable = false, length = 50)
    private String channel;

    @Column(length = 50)
    private String slackTimestamp;

    @Column(length = 50)
    private String threadTs;

    @Column(length = 50)
    private String channelType;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime slackMessageTime;
}