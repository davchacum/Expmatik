package com.expmatik.backend.chat;

import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.expmatik.backend.chat.DTOs.ChatResponse;

@Component
public class ChatRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishMessage(String adminEmail, String maintainerEmail, UUID maintenanceId, ChatResponse chatResponse) {
        String destination = "/queue/chat/" + maintenanceId;
        messagingTemplate.convertAndSendToUser(adminEmail, destination, chatResponse);
        messagingTemplate.convertAndSendToUser(maintainerEmail, destination, chatResponse);
    }

}
