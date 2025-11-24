package com.nixo.fde.slackbot.repository;

import com.nixo.fde.slackbot.models.SlackTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlackTicketRepository extends JpaRepository<SlackTicket, Long> {

    List<SlackTicket> findAllByOrderByUpdatedAtDesc();

    List<SlackTicket> findByStatus(String status);

    List<SlackTicket> findByCategory(String category);

    @Query("SELECT t FROM SlackTicket t WHERE t.updatedAt >= :since ORDER BY t.updatedAt DESC")
    List<SlackTicket> findRecentTickets(@Param("since") LocalDateTime since);

    @Query("SELECT t FROM SlackTicket t LEFT JOIN FETCH t.messages WHERE t.id = :id")
    SlackTicket findByIdWithMessages(@Param("id") Long id);

    @Query("SELECT COUNT(t) FROM SlackTicket t WHERE t.status = 'OPEN'")
    long countOpenTickets();
}