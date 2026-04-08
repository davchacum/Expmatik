package com.expmatik.backend.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;

import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.user.User;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationRealtimePublisher notificationRealtimePublisher;

    @Spy
    @InjectMocks
    private NotificationService notificationService;


    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
        notification.setMessage("Test notification");
        notification.setType(NotificationType.ASSIGNED_RESTOCKING);
        notification.setLink("/test-link");
        notification.setIsRead(false);
    }

    @Nested
    @DisplayName("findById")
    class FindById {
        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should return notification when ID is valid and user is authorized")
            void testFindById_validIdAndAuthorizedUser_shouldReturnNotification() {
                User user = new User();
                user.setId(UUID.randomUUID());
                notification.setUser(user);

                when(notificationRepository.findById(any(UUID.class))).thenReturn(Optional.of(notification));

                Notification result = notificationService.findById(UUID.randomUUID(), user);

                assertEquals(result.getMessage(), "Test notification");
                assertEquals(result.getType(), NotificationType.ASSIGNED_RESTOCKING);
                assertEquals(result.getLink(), "/test-link");
                verify(notificationRepository).findById(any(UUID.class));
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {
            @Test
            @DisplayName("should throw ResourceNotFoundException when notification does not exist")
            void testFindById_notificationDoesNotExist_shouldThrowResourceNotFoundException() {
                when(notificationRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> notificationService.findById(UUID.randomUUID(), new User()));
            }

            @Test
            @DisplayName("should throw AccessDeniedException when user is not authorized")
            void testFindById_userNotAuthorized_shouldThrowAccessDeniedException() {
                User user = new User();
                user.setId(UUID.randomUUID());
                User notificationOwner = new User();
                notificationOwner.setId(UUID.randomUUID());
                notification.setUser(notificationOwner);

                when(notificationRepository.findById(any(UUID.class))).thenReturn(Optional.of(notification));

                assertThrows(AccessDeniedException.class, () -> notificationService.findById(UUID.randomUUID(), user));
            }
        }
    }

    @Nested
    @DisplayName("createNotification")
    class CreateNotification {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {
            @Test
            @DisplayName("should create and return notification when input is valid")
            void testCreateNotification_validInput_shouldCreateAndReturnNotification() {
                User user = new User();
                user.setId(UUID.randomUUID());
                notification.setUser(user);

                when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

                Notification result = notificationService.createNotification(NotificationType.ASSIGNED_RESTOCKING, "Test message", "/test-link", user);

                assertEquals(result.getMessage(), "Test notification");
                assertEquals(result.getType(), NotificationType.ASSIGNED_RESTOCKING);
                assertEquals(result.getLink(), "/test-link");
                assertEquals(result.getUser(), user);
                verify(notificationRepository).save(any(Notification.class));
                verify(notificationRealtimePublisher).publishUnreadCount(user);
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
            @DisplayName("should mark notification as read when ID is valid and user is authorized")
            void testMarkAsRead_validIdAndAuthorizedUser_shouldMarkAsRead() {
                User user = new User();
                user.setId(UUID.randomUUID());
                notification.setUser(user);

                when(notificationRepository.findById(any(UUID.class))).thenReturn(Optional.of(notification));
                when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

                Notification result = notificationService.markAsRead(UUID.randomUUID(), user);

                assertEquals(result.getIsRead(), true);
                verify(notificationRepository).findById(any(UUID.class));
                verify(notificationRepository).save(any(Notification.class));
                verify(notificationRealtimePublisher).publishUnreadCount(user);
            }
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when notification does not exist")
        void testMarkAsRead_notificationDoesNotExist_shouldThrowResourceNotFoundException() {
            when(notificationRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(UUID.randomUUID(), new User()));
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user is not authorized")
        void testMarkAsRead_userNotAuthorized_shouldThrowAccessDeniedException() {
            User user = new User();
            user.setId(UUID.randomUUID());
            User notificationOwner = new User();
            notificationOwner.setId(UUID.randomUUID());
            notification.setUser(notificationOwner);

            when(notificationRepository.findById(any(UUID.class))).thenReturn(Optional.of(notification));

            assertThrows(AccessDeniedException.class, () -> notificationService.markAsRead(UUID.randomUUID(), user));
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("should mark all notifications as read for the user")
            void testMarkAllAsRead_shouldMarkAllNotificationsAsRead() {
                User user = new User();
                user.setId(UUID.randomUUID());
                notification.setUser(user);

                Page<Notification> unreadNotifications = new PageImpl<>(List.of(notification));

                when(notificationRepository.searchNotifications(any(UUID.class), eq(false), isNull(), isNull(), isNull(), any())).thenReturn(unreadNotifications);
                when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

                notificationService.markAllAsRead(user);

                verify(notificationRepository).searchNotifications(any(UUID.class), eq(false), isNull(), isNull(), isNull(), any());
                verify(notificationRepository).save(notification);
                verify(notificationRealtimePublisher).publishUnreadCount(user);
                assertEquals(notification.getIsRead(), true);
            }
        }
    }



}
