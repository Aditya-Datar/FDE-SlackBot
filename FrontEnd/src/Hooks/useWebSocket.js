import { useEffect, useState, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

export const useWebSocket = () => {
  const [tickets, setTickets] = useState([]);
  const [isConnected, setIsConnected] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [newTicketIds, setNewTicketIds] = useState(new Set());
  const [updatedTicketIds, setUpdatedTicketIds] = useState(new Set());
  const clientRef = useRef(null);

  // Fetch initial tickets
  useEffect(() => {
    const fetchInitialTickets = async () => {
      try {
        setIsLoading(true);
        console.log('Fetching initial tickets...');
        const response = await axios.get(`${API_BASE}/tickets`);
        console.log('Initial tickets:', response.data);
        
        const transformedTickets = response.data.map(ticket => ({
          id: ticket.id.toString(),
          title: ticket.title,
          type: ticket.category, // Backend sends 'category', Frontend expects 'type' usually, make sure this matches your TicketCard
          timestamp: ticket.updatedAt,
          customerName: ticket.customerName || 'Customer',
          messageCount: ticket.messageCount || (ticket.messages ? ticket.messages.length : 0),
          messages: ticket.messages || []
        }));
        
        setTickets(transformedTickets);
      } catch (error) {
        console.error('Error fetching initial tickets:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchInitialTickets();
  }, []);

  // Setup WebSocket
  useEffect(() => {
    const socket = new SockJS('http://localhost:8080/ws');
    
    const client = new Client({
      webSocketFactory: () => socket,
      onConnect: () => {
        console.log('Connected to WebSocket');
        setIsConnected(true);

        client.subscribe('/topic/tickets', (message) => {
          const payload = JSON.parse(message.body);
          console.log('WebSocket message received:', payload);
          handleWebSocketMessage(payload);
        });
      },
      onDisconnect: () => {
        console.log('Disconnected from WebSocket');
        setIsConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) clientRef.current.deactivate();
    };
  }, []);

  const handleWebSocketMessage = (msg) => {
    const { type, data } = msg;

    // Transform incoming WS data to match Frontend shape
    const transformedTicket = {
      id: data.id.toString(),
      title: data.title,
      type: data.category,
      timestamp: data.updatedAt,
      customerName: data.customerName || 'Customer',
      messageCount: data.messageCount || (data.messages ? data.messages.length : 0),
      messages: data.messages || [] // Ensure messages are passed for the live view
    };

    setTickets((prev) => {
      if (type === 'TICKET_CREATED') {
        // Handle New Ticket Badge
        setNewTicketIds(ids => new Set(ids).add(transformedTicket.id));
        setTimeout(() => {
          setNewTicketIds(ids => {
            const newSet = new Set(ids);
            newSet.delete(transformedTicket.id);
            return newSet;
          });
        }, 5000);

        showNotification('New Ticket Created', transformedTicket.title);
        
        // Prevent duplicate add if it already exists
        if (prev.some(t => t.id === transformedTicket.id)) return prev;

        // Add to TOP
        return [transformedTicket, ...prev];

      } else if (type === 'TICKET_UPDATED') {
        // Handle Updated Badge
        setUpdatedTicketIds(ids => new Set(ids).add(transformedTicket.id));
        setTimeout(() => {
          setUpdatedTicketIds(ids => {
            const newSet = new Set(ids);
            newSet.delete(transformedTicket.id);
            return newSet;
          });
        }, 5000);

        showNotification('Ticket Updated', transformedTicket.title);
        
        // --- KEY CHANGE: SORTING LOGIC ---
        // 1. Remove the old version of this ticket
        const others = prev.filter((t) => t.id !== transformedTicket.id);
        
        // 2. Add the updated version to the VERY TOP
        return [transformedTicket, ...others];
      }
      return prev;
    });
  };

  const showNotification = (title, body) => {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(title, { 
        body,
        icon: '/favicon.ico',
        badge: '/favicon.ico'
      });
    }
  };

  return { 
    tickets: tickets.map(t => ({
      ...t,
      isNew: newTicketIds.has(t.id),
      isUpdated: updatedTicketIds.has(t.id)
    })), 
    isConnected,
    isLoading 
  };
};