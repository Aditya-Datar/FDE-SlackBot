package com.nixo.fde.slackbot.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "slack_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlackTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 20)
    @Builder.Default
    private String status = "OPEN";

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SlackMessage> messages = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void addMessage(SlackMessage message) {
        messages.add(message);
        message.setTicket(this);
    }

    public void removeMessage(SlackMessage message) {
        messages.remove(message);
        message.setTicket(null);
    }

    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }
}