package com.nixo.fde.slackbot.controller;

import com.nixo.fde.slackbot.models.SlackTicket;
import com.nixo.fde.slackbot.payload.SlackTicketDto;
import com.nixo.fde.slackbot.payload.SlackTicketStatusDto;
import com.nixo.fde.slackbot.repository.SlackTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class SlackTicketController {
    private final SlackTicketRepository slackTicketRepository;

    @GetMapping
    public ResponseEntity<List<SlackTicketDto>> getAllTickets() {
        log.info("Fetching all tickets");
        List<SlackTicketDto> tickets = slackTicketRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(SlackTicketDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SlackTicketDto> getTicketById(@PathVariable Long id) {
        log.info("Fetching ticket by id: {}", id);
        SlackTicket ticket = slackTicketRepository.findByIdWithMessages(id);

        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(SlackTicketDto.fromEntityWithMessages(ticket));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<SlackTicketDto>> getTicketsByStatus(@PathVariable String status) {
        log.info("Fetching tickets by status: {}", status);
        List<SlackTicketDto> tickets = slackTicketRepository.findByStatus(status.toUpperCase())
                .stream()
                .map(SlackTicketDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<SlackTicketDto>> getTicketsByCategory(@PathVariable String category) {
        log.info("Fetching tickets by category: {}", category);
        List<SlackTicketDto> tickets = slackTicketRepository.findByCategory(category.toUpperCase())
                .stream()
                .map(SlackTicketDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tickets);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SlackTicketDto> updateTicketStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        log.info("Updating ticket {} status to {}", id, status);
        return slackTicketRepository.findById(id)
                .map(ticket -> {
                    ticket.setStatus(status.toUpperCase());
                    SlackTicket saved = slackTicketRepository.save(ticket);
                    return ResponseEntity.ok(SlackTicketDto.fromEntity(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<SlackTicketStatusDto> getStats() {
        long openCount = slackTicketRepository.countOpenTickets();
        long totalCount = slackTicketRepository.count();

        return ResponseEntity.ok(new SlackTicketStatusDto(totalCount, openCount));
    }
}
