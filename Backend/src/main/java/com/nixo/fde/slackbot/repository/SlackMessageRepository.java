package com.nixo.fde.slackbot.repository;

import com.nixo.fde.slackbot.models.SlackMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlackMessageRepository extends JpaRepository<SlackMessage, Long> {

    List<SlackMessage> findBySlackTimestamp(String slackTimestamp);

    boolean existsBySlackTimestamp(String slackTimestamp);

    List<SlackMessage> findByThreadTs(String threadTs);

    List<SlackMessage> findByTicketId(Long ticketId);

    @Query("SELECT m FROM SlackMessage m WHERE m.slackMessageTime >= :since AND m.embedding IS NOT NULL")
    List<SlackMessage> findRecentMessagesWithEmbeddings(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(m) FROM SlackMessage m WHERE m.ticket.id = :ticketId")
    int countByTicketId(@Param("ticketId") Long ticketId);
}