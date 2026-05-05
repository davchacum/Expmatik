package com.expmatik.backend.chat;

import java.time.LocalDateTime;
import java.util.UUID;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.chat.DTOs.ChatResponse;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.maintenance.Maintenance;
import com.expmatik.backend.maintenance.MaintenanceService;
import com.expmatik.backend.maintenance.MaintenanceStatus;
import com.expmatik.backend.user.User;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final MaintenanceService maintenanceService;
    private final ChatRealtimePublisher chatRealtimePublisher;

    public ChatService(ChatRepository chatRepository, MaintenanceService maintenanceService, ChatRealtimePublisher chatRealtimePublisher) {
        this.chatRepository = chatRepository;
        this.maintenanceService = maintenanceService;
        this.chatRealtimePublisher = chatRealtimePublisher;
    }

    private void validateDontEndedMaintenance(Maintenance maintenance) {
        if (maintenance.getStatus() == MaintenanceStatus.COMPLETED || maintenance.getStatus() == MaintenanceStatus.REJECTED_EXPIRED) {
            throw new ConflictException("Cannot send messages to a completed or rejected maintenance.") ;
        }
    }

    @Transactional
    public void sendMessage(UUID maintenanceId, String content, User sender) {
        Maintenance maintenance = maintenanceService.findById(maintenanceId, sender);
        validateDontEndedMaintenance(maintenance);
        Chat chat = new Chat();
        chat.setContent(content);
        chat.setSentAt(LocalDateTime.now());
        chat.setMaintenance(maintenance);
        chat.setSender(sender);
        Chat saved = chatRepository.save(chat);
        ChatResponse response = ChatResponse.fromChat(saved);
        chatRealtimePublisher.publishMessage(
            maintenance.getAdministrator().getEmail(),
            maintenance.getMaintainer().getEmail(),
            maintenanceId,
            response
        );
    }

    @Transactional(readOnly = true)
    public List<Chat> getMessagesByMaintenance(UUID maintenanceId, User user) {
        maintenanceService.findById(maintenanceId, user);
        return chatRepository.findByMaintenanceId(maintenanceId);
    }

    @Transactional
    public Chat markAsRead(UUID chatId, User user) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new ResourceNotFoundException("Chat message not found with id: " + chatId));
        Maintenance maintenance = maintenanceService.findById(chat.getMaintenance().getId(), user);
        if (chat.getSender().getId().equals(user.getId())) {
            throw new AccessDeniedException("Cannot mark your own message as read.");
        }
        chat.setReadAt(LocalDateTime.now());
        Chat saved = chatRepository.save(chat);
        chatRealtimePublisher.publishMessage(
            maintenance.getAdministrator().getEmail(),
            maintenance.getMaintainer().getEmail(),
            maintenance.getId(),
            ChatResponse.fromChat(saved)
        );
        return saved;
    }

}
