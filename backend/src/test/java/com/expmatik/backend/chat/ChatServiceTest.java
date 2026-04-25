package com.expmatik.backend.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.expmatik.backend.chat.DTOs.ChatResponse;
import com.expmatik.backend.exceptions.ConflictException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.maintenance.Maintenance;
import com.expmatik.backend.maintenance.MaintenanceService;
import com.expmatik.backend.maintenance.MaintenanceStatus;
import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private MaintenanceService maintenanceService;

    @Mock
    private ChatRealtimePublisher chatRealtimePublisher;

    @Spy
    @InjectMocks
    private ChatService chatService;

    private User maintainer;
    private User administrator;
    private User otherUser;
    private Maintenance maintenance;
    private Chat chat;

    @BeforeEach
    void setUp() {
        maintainer = new User();
        maintainer.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        maintainer.setEmail("maintainer@test.com");
        maintainer.setRole(Role.MAINTAINER);

        administrator = new User();
        administrator.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        administrator.setEmail("admin@test.com");
        administrator.setRole(Role.ADMINISTRATOR);

        otherUser = new User();
        otherUser.setId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        otherUser.setEmail("other@test.com");
        otherUser.setRole(Role.MAINTAINER);

        maintenance = new Maintenance();
        maintenance.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        maintenance.setAdministrator(administrator);
        maintenance.setMaintainer(maintainer);

        chat = new Chat();
        chat.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        chat.setContent("Hello");
        chat.setSentAt(LocalDateTime.now());
        chat.setMaintenance(maintenance);
        chat.setSender(maintainer);
    }

    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should save and publish message when sender is the maintainer")
            void testSendMessage_senderIsMaintainer_shouldSaveAndPublish() {
                when(maintenanceService.findById(any(UUID.class), any(User.class))).thenReturn(maintenance);
                when(chatRepository.save(any(Chat.class))).thenReturn(chat);

                chatService.sendMessage(maintenance.getId(), "Hello", maintainer);

                verify(chatRepository).save(any(Chat.class));
                verify(chatRealtimePublisher).publishMessage(
                    eq(administrator.getEmail()),
                    eq(maintainer.getEmail()),
                    eq(maintenance.getId()),
                    any(ChatResponse.class)
                );
            }

            @Test
            @DisplayName("should save and publish message when sender is the administrator")
            void testSendMessage_senderIsAdministrator_shouldSaveAndPublish() {
                chat.setSender(administrator);
                when(maintenanceService.findById(any(UUID.class), any(User.class))).thenReturn(maintenance);
                when(chatRepository.save(any(Chat.class))).thenReturn(chat);

                chatService.sendMessage(maintenance.getId(), "Hello admin", administrator);

                verify(chatRepository).save(any(Chat.class));
                verify(chatRealtimePublisher).publishMessage(
                    eq(administrator.getEmail()),
                    eq(maintainer.getEmail()),
                    eq(maintenance.getId()),
                    any(ChatResponse.class)
                );
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw ResourceNotFoundException when maintenance does not exist")
            void testSendMessage_maintenanceDoesNotExist_shouldThrowResourceNotFoundException() {
                when(maintenanceService.findById(any(UUID.class), any(User.class)))
                    .thenThrow(new ResourceNotFoundException("Maintenance not found"));

                assertThrows(ResourceNotFoundException.class, () ->
                    chatService.sendMessage(UUID.randomUUID(), "Hello", maintainer));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when sender is not a participant of the maintenance")
            void testSendMessage_senderIsNotParticipant_shouldThrowAccessDeniedException() {
                when(maintenanceService.findById(any(UUID.class), any(User.class)))
                    .thenThrow(new AccessDeniedException("Not authorized"));

                assertThrows(AccessDeniedException.class, () ->
                    chatService.sendMessage(UUID.randomUUID(), "Hello", otherUser));
            }

            @Test
            @DisplayName("should throw ConflictException when maintenance is completed")
            void testSendMessage_maintenanceCompleted_shouldThrowConflictException() {
                maintenance.setStatus(MaintenanceStatus.COMPLETED);
                when(maintenanceService.findById(any(UUID.class), any(User.class))).thenReturn(maintenance);

                assertThrows(ConflictException.class, () ->
                    chatService.sendMessage(maintenance.getId(), "Hello", maintainer));
            }

            @Test
            @DisplayName("should throw ConflictException when maintenance is REJECTED_EXPIRED")
            void testSendMessage_maintenanceRejectedExpired_shouldThrowConflictException() {
                maintenance.setStatus(MaintenanceStatus.REJECTED_EXPIRED);
                when(maintenanceService.findById(any(UUID.class), any(User.class))).thenReturn(maintenance);

                assertThrows(ConflictException.class, () ->
                    chatService.sendMessage(maintenance.getId(), "Hello", maintainer));
            }

        }
    }

    @Nested
    @DisplayName("getMessagesByMaintenance")
    class GetMessagesByMaintenance {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should return list of messages when user is authorized")
            void testGetMessagesByMaintenance_authorizedUser_shouldReturnMessageList() {
                when(maintenanceService.findById(any(UUID.class), any(User.class))).thenReturn(maintenance);
                when(chatRepository.findByMaintenanceId(any(UUID.class))).thenReturn(List.of(chat));

                List<Chat> result = chatService.getMessagesByMaintenance(maintenance.getId(), administrator);

                assertEquals(1, result.size());
                assertEquals(chat.getContent(), result.get(0).getContent());
                verify(chatRepository).findByMaintenanceId(maintenance.getId());
            }

            @Test
            @DisplayName("should return empty list when there are no messages")
            void testGetMessagesByMaintenance_noMessages_shouldReturnEmptyList() {
                when(maintenanceService.findById(any(UUID.class), any(User.class))).thenReturn(maintenance);
                when(chatRepository.findByMaintenanceId(any(UUID.class))).thenReturn(List.of());

                List<Chat> result = chatService.getMessagesByMaintenance(maintenance.getId(), administrator);

                assertEquals(0, result.size());
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw ResourceNotFoundException when maintenance does not exist")
            void testGetMessagesByMaintenance_maintenanceDoesNotExist_shouldThrowResourceNotFoundException() {
                when(maintenanceService.findById(any(UUID.class), any(User.class)))
                    .thenThrow(new ResourceNotFoundException("Maintenance not found"));

                assertThrows(ResourceNotFoundException.class, () ->
                    chatService.getMessagesByMaintenance(UUID.randomUUID(), administrator));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when user is not a participant")
            void testGetMessagesByMaintenance_userNotParticipant_shouldThrowAccessDeniedException() {
                when(maintenanceService.findById(any(UUID.class), any(User.class)))
                    .thenThrow(new AccessDeniedException("Not authorized"));

                assertThrows(AccessDeniedException.class, () ->
                    chatService.getMessagesByMaintenance(UUID.randomUUID(), otherUser));
            }
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should mark message as read when user is the recipient")
            void testMarkAsRead_userIsRecipient_shouldSetReadAt() {
                when(chatRepository.findById(any(UUID.class))).thenReturn(Optional.of(chat));
                when(maintenanceService.findById(any(UUID.class), any(User.class))).thenReturn(maintenance);
                when(chatRepository.save(any(Chat.class))).thenReturn(chat);

                Chat result = chatService.markAsRead(chat.getId(), administrator);

                assertNotNull(result.getReadAt());
                verify(chatRepository).save(any(Chat.class));
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("should throw ResourceNotFoundException when chat does not exist")
            void testMarkAsRead_chatDoesNotExist_shouldThrowResourceNotFoundException() {
                when(chatRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () ->
                    chatService.markAsRead(UUID.randomUUID(), administrator));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when user is not a participant of the maintenance")
            void testMarkAsRead_userNotParticipant_shouldThrowAccessDeniedException() {
                when(chatRepository.findById(any(UUID.class))).thenReturn(Optional.of(chat));
                when(maintenanceService.findById(any(UUID.class), any(User.class)))
                    .thenThrow(new AccessDeniedException("Not authorized"));

                assertThrows(AccessDeniedException.class, () ->
                    chatService.markAsRead(chat.getId(), otherUser));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when sender tries to mark their own message as read")
            void testMarkAsRead_senderTriesToMarkOwnMessage_shouldThrowAccessDeniedException() {
                when(chatRepository.findById(any(UUID.class))).thenReturn(Optional.of(chat));
                when(maintenanceService.findById(any(UUID.class), any(User.class))).thenReturn(maintenance);

                assertThrows(AccessDeniedException.class, () ->
                    chatService.markAsRead(chat.getId(), maintainer));
            }
        }
    }
}
