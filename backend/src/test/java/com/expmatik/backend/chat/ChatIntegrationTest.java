package com.expmatik.backend.chat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/chats/maintenance/{maintenanceId}")
    class GetMessagesByMaintenance {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should return messages when administrator has access")
            @WithUserDetails("admin@expmatik.com")
            void testGetMessagesByMaintenance_AdminHasAccess_shouldReturnMessages() throws Exception {
                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/chats/maintenance/{maintenanceId}", maintenanceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].content").value("Hola administrador"))
                    .andExpect(jsonPath("$[0].sender.email").value("repo@expmatik.com"))
                    .andExpect(jsonPath("$[1].content").value("Hola reponedor"))
                    .andExpect(jsonPath("$[1].sender.email").value("admin@expmatik.com"));
            }

            @Test
            @DisplayName("Should return messages when maintainer has access")
            @WithUserDetails("repo@expmatik.com")
            void testGetMessagesByMaintenance_MaintainerHasAccess_shouldReturnMessages() throws Exception {
                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/chats/maintenance/{maintenanceId}", maintenanceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
            }

            @Test
            @DisplayName("Should return empty list when maintenance has no messages")
            @WithUserDetails("admin@expmatik.com")
            void testGetMessagesByMaintenance_NoMessages_shouldReturnEmptyList() throws Exception {
                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000006");

                mockMvc.perform(get("/api/chats/maintenance/{maintenanceId}", maintenanceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should return 404 when maintenance does not exist")
            @WithUserDetails("admin@expmatik.com")
            void testGetMessagesByMaintenance_MaintenanceNotFound_shouldReturn404() throws Exception {
                UUID nonExistingId = UUID.fromString("00000000-0000-0000-0000-000000000999");

                mockMvc.perform(get("/api/chats/maintenance/{maintenanceId}", nonExistingId))
                    .andExpect(status().isNotFound());
            }

            @Test
            @DisplayName("Should return 403 when user is not a participant of the maintenance")
            @WithUserDetails("admin2@expmatik.com")
            void testGetMessagesByMaintenance_UserNotParticipant_shouldReturn403() throws Exception {
                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/chats/maintenance/{maintenanceId}", maintenanceId))
                    .andExpect(status().isForbidden());
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/chats/{id}/read")
    class MarkAsRead {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should mark message as read when administrator is the recipient")
            @WithUserDetails("admin@expmatik.com")
            void testMarkAsRead_AdminIsRecipient_shouldSetReadAt() throws Exception {
                UUID chatId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(patch("/api/chats/{id}/read", chatId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(chatId.toString()))
                    .andExpect(jsonPath("$.content").value("Hola administrador"))
                    .andExpect(jsonPath("$.readAt").exists());
            }

            @Test
            @DisplayName("Should mark message as read when maintainer is the recipient")
            @WithUserDetails("repo@expmatik.com")
            void testMarkAsRead_MaintainerIsRecipient_shouldSetReadAt() throws Exception {
                UUID chatId = UUID.fromString("00000000-0000-0000-0000-000000000002");

                mockMvc.perform(patch("/api/chats/{id}/read", chatId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(chatId.toString()))
                    .andExpect(jsonPath("$.content").value("Hola reponedor"))
                    .andExpect(jsonPath("$.readAt").exists());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should return 404 when chat message does not exist")
            @WithUserDetails("admin@expmatik.com")
            void testMarkAsRead_ChatNotFound_shouldReturn404() throws Exception {
                UUID nonExistingId = UUID.fromString("00000000-0000-0000-0000-000000000999");

                mockMvc.perform(patch("/api/chats/{id}/read", nonExistingId))
                    .andExpect(status().isNotFound());
            }

            @Test
            @DisplayName("Should return 403 when user is not a participant of the maintenance")
            @WithUserDetails("admin2@expmatik.com")
            void testMarkAsRead_UserNotParticipant_shouldReturn403() throws Exception {
                UUID chatId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(patch("/api/chats/{id}/read", chatId))
                    .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("Should return 403 when sender tries to mark their own message as read")
            @WithUserDetails("repo@expmatik.com")
            void testMarkAsRead_SenderMarksOwnMessage_shouldReturn403() throws Exception {
                UUID chatId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(patch("/api/chats/{id}/read", chatId))
                    .andExpect(status().isForbidden());
            }
        }
    }
}
