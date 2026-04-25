package com.expmatik.backend.chat;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.chat.DTOs.ChatMessagePayload;
import com.expmatik.backend.chat.DTOs.ChatResponse;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/chats")
@Tag(name = "ChatController", description = "Controlador para gestionar el chat asociado a los mantenimientos.")
@Validated
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    @MessageMapping("/chat/{maintenanceId}")
    public void sendMessage(@DestinationVariable UUID maintenanceId, ChatMessagePayload payload, Principal principal) {
        User sender = userService.findByEmail(principal.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + principal.getName()));
        chatService.sendMessage(maintenanceId, payload.content(), sender);
    }

    @GetMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<?> getMessagesByMaintenance(@PathVariable UUID maintenanceId) {
        User user = userService.getUserProfile();
        return ResponseEntity.ok(ChatResponse.fromChatList(chatService.getMessagesByMaintenance(maintenanceId, user)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable UUID id) {
        User user = userService.getUserProfile();
        return ResponseEntity.ok(ChatResponse.fromChat(chatService.markAsRead(id, user)));
    }

}
