package com.expmatik.backend.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.jwt.UserDetailsImpl;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // == Test findCurrentUser ==

    @Nested
    @DisplayName("findCurrentUser")
    class FindCurrentUser {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("findCurrentUser should return username when authenticated")
            void findCurrentUser_shouldReturnUsernameWhenAuthenticated() {
                UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
                when(userDetails.getUsername()).thenReturn("test@email.com");
                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);

                String username = userService.findCurrentUser();

                assertEquals("test@email.com", username);
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("findCurrentUser should throw when authentication is null")
            void findCurrentUser_shouldThrowWhenAuthenticationNull() {
                when(securityContext.getAuthentication()).thenReturn(null);
                SecurityContextHolder.setContext(securityContext);

                assertThrows(AuthenticationException.class, () -> userService.findCurrentUser());
            }

            @Test
            @DisplayName("findCurrentUser should throw when principal is not UserDetails")
            void findCurrentUser_shouldThrowWhenPrincipalNotUserDetails() {
                when(authentication.getPrincipal()).thenReturn("someString");
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);

                assertThrows(AuthenticationException.class, () -> userService.findCurrentUser());
            }
        }
    }

    // == Test getUserProfile ==

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfile {

        @Nested
        @DisplayName("Success Cases")
        class SuccessCases {

            @Test
            @DisplayName("getUserProfile should return user when exists")
            void getUserProfile_shouldReturnUserWhenExists() {
                User user = new User();
                user.setEmail("test@email.com");

                UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
                when(userDetails.getUsername()).thenReturn("test@email.com");
                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);

                when(userRepository.findByEmail("test@email.com"))
                        .thenReturn(Optional.of(user));

                User result = userService.getUserProfile();

                assertNotNull(result);
                assertEquals("test@email.com", result.getEmail());
            }
        }

        @Nested
        @DisplayName("Failure Cases")
        class FailureCases {

            @Test
            @DisplayName("getUserProfile should throw when user not found")
            void getUserProfile_shouldThrowWhenUserNotFound() {
                UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
                when(userDetails.getUsername()).thenReturn("test@email.com");
                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);

                when(userRepository.findByEmail("test@email.com"))
                        .thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> userService.getUserProfile());
            }
        }
    }
}
