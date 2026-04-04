package com.expmatik.backend.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.expmatik.backend.notification.DTOs.NotificationCreate;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("GET /api/notifications/{id}")
    class GetNotificationById {
        
        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("When notification exists and belongs to user, should return notification")
            @WithUserDetails("admin@expmatik.com")
            void testGetNotificationById_ValidIdAndBelongsToUser_shouldReturnNotification() throws Exception {
                UUID notificationId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/notifications/{id}", notificationId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(notificationId.toString()))
                        .andExpect(jsonPath("$.message").value("El producto Leche Entera está a punto de caducar."))
                        .andExpect(jsonPath("$.type").value("EXPIRATION_WARNING"))
                        .andExpect(jsonPath("$.isRead").value(false));
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("When notification does not exist, should return 404")
            @WithUserDetails("admin@expmatik.com")
            void testGetNotificationById_NotValidId_shouldThrowResourceNotFoundException() throws Exception {
                UUID notificationId = UUID.fromString("00000000-0000-0000-0000-000000000099");

                mockMvc.perform(get("/api/notifications/{id}", notificationId))
                        .andExpect(status().isNotFound());
            }

            @Test
            @DisplayName("When notification belongs to another user, should return 403")
            @WithUserDetails("admin2@expmatik.com")
            void testGetNotificationById_ValidIdButBelongsToAnotherUser_shouldThrowAccessDeniedException() throws Exception {
                UUID notificationId = UUID.fromString("00000000-0000-0000-0000-000000000001");

                mockMvc.perform(get("/api/notifications/{id}", notificationId))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread-count")
    class GetUnreadNotificationsCount {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("When user has unread notifications, should return count")
            @WithUserDetails("admin@expmatik.com")
            void testGetUnreadNotificationCount_ValidIdAndBelongsToUser_shouldReturnCount() throws Exception {
                mockMvc.perform(get("/api/notifications/unread/count"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").value(1));
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications/{id}/mark-as-read")
    class MarkNotificationAsRead {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("When notification exists and belongs to user, should mark as read")
            @WithUserDetails("admin@expmatik.com")
            void testMarkNotificationAsRead_ValidIdAndBelongsToUser_shouldMarkAsRead() throws Exception {
                UUID notificationId = UUID.fromString("00000000-0000-0000-0000-000000000002");

                mockMvc.perform(patch("/api/notifications/{id}/mark-as-read", notificationId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(notificationId.toString()))
                        .andExpect(jsonPath("$.isRead").value(true));
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("When notification does not exist, should return 404")
            @WithUserDetails("admin@expmatik.com")
            void testMarkNotificationAsRead_NotValidId_shouldThrowResourceNotFoundException() throws Exception {
                UUID notificationId = UUID.fromString("00000000-0000-0000-0000-000000000099");
                mockMvc.perform(patch("/api/notifications/{id}/mark-as-read", notificationId))
                        .andExpect(status().isNotFound());
            }

            @Test
            @DisplayName("When notification belongs to another user, should return 403")
            @WithUserDetails("admin2@expmatik.com")
            void testMarkNotificationAsRead_ValidIdButBelongsToAnotherUser_shouldThrowAccessDeniedException() throws Exception {
                UUID notificationId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                mockMvc.perform(patch("/api/notifications/{id}/mark-as-read", notificationId))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications/mark-all-as-read")
    class MarkAllNotificationsAsRead {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("When user has unread notifications, should mark all as read")
            @WithUserDetails("admin@expmatik.com")
            void testMarkAllNotificationsAsRead_ValidUser_shouldMarkAllAsRead() throws Exception {
                mockMvc.perform(patch("/api/notifications/mark-all-as-read"))
                        .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/notifications/unread/count"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").value(0));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/notifications")
    class SearchNotifications {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("When user has notifications, should return notifications list")
            @WithUserDetails("admin@expmatik.com")
            void testSearchNotifications_ValidUser_shouldReturnNotificationsList() throws Exception {
                mockMvc.perform(get("/api/notifications"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].id").value("00000000-0000-0000-0000-000000000002"))
                        .andExpect(jsonPath("$.content[0].message").value("El producto Leche Entera está caducado."))
                        .andExpect(jsonPath("$.content[0].type").value("PRODUCT_EXPIRED"))
                        .andExpect(jsonPath("$.content[0].isRead").value(true))
                        .andExpect(jsonPath("$.content[0].createdAt").exists())
                        .andExpect(jsonPath("$.content[0].userId").value("00000000-0000-0000-0000-000000000001"))
                        .andExpect(jsonPath("$.content[1].id").value("00000000-0000-0000-0000-000000000001"))
                        .andExpect(jsonPath("$.content[1].message").value("El producto Leche Entera está a punto de caducar."))
                        .andExpect(jsonPath("$.content[1].type").value("EXPIRATION_WARNING"))
                        .andExpect(jsonPath("$.content[1].isRead").value(false))
                        .andExpect(jsonPath("$.content[1].createdAt").exists())
                        .andExpect(jsonPath("$.content[1].userId").value("00000000-0000-0000-0000-000000000001"))
                        .andExpect(jsonPath("$.pageable").exists())
                        .andExpect(jsonPath("$.totalElements").value(2));
            }
            
            @Test
            @DisplayName("When user has no notifications, should return empty list")
            @WithUserDetails("admin2@expmatik.com")
            void testSearchNotifications_NoNotifications_shouldReturnEmptyList() throws Exception {
                mockMvc.perform(get("/api/notifications"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content").isEmpty());
            }

            @Test
            @DisplayName("When user has notifications and filters by isRead, should return filtered notifications list")
            @WithUserDetails("admin@expmatik.com")
            void testSearchNotifications_FilteredByIsRead_shouldReturnFilteredNotificationsList() throws Exception {
                Boolean isRead = false;
                mockMvc.perform(get("/api/notifications")
                        .param("isRead", isRead.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.content[0].id").value("00000000-0000-0000-0000-000000000001"));
            }

            @Test
            @DisplayName("When user has notifications and filters by type, should return filtered notifications list")
            @WithUserDetails("admin@expmatik.com")
            void testSearchNotifications_FilteredByType_shouldReturnFilteredNotificationsList() throws Exception {
                NotificationType type = NotificationType.EXPIRATION_WARNING;
                mockMvc.perform(get("/api/notifications")
                        .param("notificationType", type.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.content[0].id").value("00000000-0000-0000-0000-000000000001"));
            }

            @Test
            @DisplayName("When user has notifications and filters by date range, should return filtered notifications list")
            @WithUserDetails("admin@expmatik.com")
            void testSearchNotifications_FilteredByDateRange_shouldReturnFilteredNotificationsList() throws Exception {
                LocalDateTime startDate = LocalDateTime.of(2024, 3, 1, 0, 0);
                LocalDateTime endDate = LocalDateTime.of(2024, 3, 1, 23, 59);
                mockMvc.perform(get("/api/notifications")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.content[0].id").value("00000000-0000-0000-0000-000000000001"));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/notifications")
    class CreateNotification {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("When request is valid, should create notification")
            @WithUserDetails("admin@expmatik.com")
            void testCreateNotification_ValidRequest_shouldCreateNotification() throws Exception {
                NotificationCreate request = new NotificationCreate(
                    "Test notification",
                    NotificationType.EXPIRATION_WARNING,
                    "/link/to/notification"
                );

                String requestBody = objectMapper.writeValueAsString(request);

                mockMvc.perform(MockMvcRequestBuilders.post("/api/notifications")
                        .contentType("application/json")
                        .content(requestBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("Test notification"))
                        .andExpect(jsonPath("$.type").value("EXPIRATION_WARNING"))
                        .andExpect(jsonPath("$.link").value("/link/to/notification"))
                        .andExpect(jsonPath("$.isRead").value(false))
                        .andExpect(jsonPath("$.userId").value("00000000-0000-0000-0000-000000000001"))
                        .andExpect(jsonPath("$.createdAt").exists());
            }
        }
    }
}