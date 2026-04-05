package com.expmatik.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.expmatik.backend.auth.DTOs.AuthResult;
import com.expmatik.backend.exceptions.InvalidHashException;
import com.expmatik.backend.exceptions.InvalidPasswordException;
import com.expmatik.backend.exceptions.InvalidRefreshTokenException;
import com.expmatik.backend.exceptions.ResourceNotFoundException;
import com.expmatik.backend.exceptions.UserExistsException;
import com.expmatik.backend.jwt.JwtService;
import com.expmatik.backend.user.RefreshToken;
import com.expmatik.backend.user.RefreshTokenService;
import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserService;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User user;

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidHashException("Error hashing token");
        }
    }

    @BeforeEach
    void setUp() {

        user = new User();
        user.setEmail("test@email.com");
        user.setPassword("hashedPassword");
        user.setRole(Role.ADMINISTRATOR);
    }

    // == Test register ==

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("register should return AuthResult when user is new")
            void testRegister_UserIsNew_ShouldReturnAuthResult() {
                when(userService.findByEmail(user.getEmail())).thenReturn(Optional.empty());
                when(passwordEncoder.encode("password")).thenReturn("hashedPassword");
                when(jwtService.generateAccessTokenFromEmail(user.getEmail())).thenReturn("accessToken");
                when(jwtService.generateRefreshTokenFromEmail(user.getEmail())).thenReturn("refreshToken");
                when(jwtService.getExpirarationFromToken("refreshToken")).thenReturn(new Date());

                AuthResult result = authService.register(
                        user.getEmail(), "password", "device1", Role.ADMINISTRATOR, "John", "Doe"
                );

                assertNotNull(result);
                assertEquals("accessToken", result.accessToken());
                assertEquals("refreshToken", result.refreshToken());
                assertEquals("ADMINISTRATOR", result.role());

                verify(userService).save(any(User.class));
                verify(refreshTokenService).save(any(RefreshToken.class));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("register should throw UserExistsException when user already exists")
            void testRegister_UserAlreadyExists_ShouldThrowUserExistsException() {
                when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

                UserExistsException exception = assertThrows(UserExistsException.class, () ->
                        authService.register(user.getEmail(), "password", "device1", Role.ADMINISTRATOR, "John", "Doe")
                );

                assertEquals("User already exists", exception.getMessage());
            }
        }
    }

    // == Test login ==

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("login should return AuthResult when password is correct")
            void testLogin_PasswordIsCorrect_ShouldReturnAuthResult() {
                when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
                when(passwordEncoder.matches("password", user.getPassword())).thenReturn(true);
                when(jwtService.generateAccessTokenFromEmail(user.getEmail())).thenReturn("accessToken");
                when(jwtService.generateRefreshTokenFromEmail(user.getEmail())).thenReturn("refreshToken");
                when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(new RefreshToken());
                when(jwtService.getExpirarationFromToken("refreshToken")).thenReturn(new Date());

                AuthResult result = authService.login(user.getEmail(), "password", "device1");

                assertNotNull(result);
                assertEquals("accessToken", result.accessToken());
                assertEquals("refreshToken", result.refreshToken());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("login should throw InvalidPasswordException when password is incorrect")
            void testLogin_PasswordIsIncorrect_ShouldThrowInvalidPasswordException() {
                when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
                when(passwordEncoder.matches("wrongPassword", user.getPassword())).thenReturn(false);

                assertThrows(InvalidPasswordException.class, () ->
                        authService.login(user.getEmail(), "wrongPassword", "device1")
                );
            }

            @Test
            @DisplayName("login should throw ResourceNotFoundException when user not found")
            void testLogin_UserNotFound_ShouldThrowResourceNotFoundException() {
                when(userService.findByEmail(user.getEmail())).thenReturn(Optional.empty());

                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                        authService.login(user.getEmail(), "password", "device1")
                );

                assertEquals("User not found", exception.getMessage());
            }
        }
    }

    // == Test refreshToken ==

    @Nested
    @DisplayName("Refresh Token")
    class RefreshTokenTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("refreshToken should return new tokens when refresh token is valid")
            void testRefreshToken_RefreshTokenIsValid_ShouldReturnNewTokens() {
                RefreshToken storedToken = new RefreshToken();
                storedToken.setToken(hashToken("oldRefreshToken"));
                storedToken.setExpiration(new Date(System.currentTimeMillis() + 10000)); // no expirado

                when(jwtService.verifyToken("oldRefreshToken")).thenReturn(true);
                when(jwtService.getEmailFromToken("oldRefreshToken")).thenReturn(user.getEmail());
                when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
                when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(storedToken);
                when(jwtService.generateAccessTokenFromEmail(user.getEmail())).thenReturn("newAccessToken");
                when(jwtService.generateRefreshTokenFromEmail(user.getEmail())).thenReturn("newRefreshToken");
                when(jwtService.getExpirarationFromToken("newRefreshToken")).thenReturn(new Date(System.currentTimeMillis() + 10000));

                AuthResult result = authService.refreshToken("oldRefreshToken", "device1");

                assertEquals("newAccessToken", result.accessToken());
                assertEquals("newRefreshToken", result.refreshToken());
            }

            @Test
            @DisplayName("refreshToken should throw InvalidRefreshTokenException when token is invalid")
            void testRefreshToken_RefreshTokenIsInvalid_ShouldThrowInvalidRefreshTokenException() {
                when(jwtService.verifyToken("invalidToken")).thenReturn(false);

                InvalidRefreshTokenException exception = assertThrows(InvalidRefreshTokenException.class, () ->
                        authService.refreshToken("invalidToken", "device1")
                );

                assertEquals("Invalid Refresh Token", exception.getMessage());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("refreshToken should throw InvalidRefreshTokenException when token does not match records")
            void testRefreshToken_TokenDoesNotMatchRecords_ShouldThrowInvalidRefreshTokenException() {
                RefreshToken storedToken = new RefreshToken();
                storedToken.setToken(hashToken("someOtherToken")); // hash diferente
                storedToken.setExpiration(new Date(System.currentTimeMillis() + 10000));

                when(jwtService.verifyToken("validToken")).thenReturn(true);
                when(jwtService.getEmailFromToken("validToken")).thenReturn(user.getEmail());
                when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
                when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(storedToken);

                InvalidRefreshTokenException exception = assertThrows(InvalidRefreshTokenException.class, () ->
                        authService.refreshToken("validToken", "device1")
                );

                assertEquals("Token does not match records", exception.getMessage());
            }

            @Test
            @DisplayName("refreshToken should throw InvalidRefreshTokenException when token is expired")
            void testRefreshToken_TokenIsExpired_ShouldThrowInvalidRefreshTokenException() {
                RefreshToken storedToken = new RefreshToken();
                storedToken.setToken(hashToken("validToken"));
                storedToken.setExpiration(new Date(System.currentTimeMillis() - 1000)); // ya expirado

                when(jwtService.verifyToken("validToken")).thenReturn(true);
                when(jwtService.getEmailFromToken("validToken")).thenReturn(user.getEmail());
                when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
                when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(storedToken);

                InvalidRefreshTokenException exception = assertThrows(InvalidRefreshTokenException.class, () ->
                        authService.refreshToken("validToken", "device1")
                );

                assertEquals("Refresh token expired", exception.getMessage());

                verify(refreshTokenService).delete(storedToken); // se elimina token expirado
            }
        }
    }

    // == Test logout ==

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("logout should delete refresh token when valid")
            void testLogout_RefreshTokenIsValid_ShouldDeleteRefreshToken() {
                RefreshToken storedToken = new RefreshToken();
                storedToken.setToken(hashToken("refreshToken"));

                when(jwtService.verifyToken("refreshToken")).thenReturn(true);
                when(jwtService.getEmailFromToken("refreshToken")).thenReturn(user.getEmail());
                when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
                when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(storedToken);

                authService.logout("refreshToken", "device1");

                verify(refreshTokenService).delete(storedToken);
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("logout should throw InvalidRefreshTokenException when token is invalid")
            void testLogout_RefreshTokenIsInvalid_ShouldThrowInvalidRefreshTokenException() {
                when(jwtService.verifyToken("invalidToken")).thenReturn(false);

                InvalidRefreshTokenException exception = assertThrows(InvalidRefreshTokenException.class, () ->
                        authService.logout("invalidToken", "device1")
                );

                assertEquals("Invalid Refresh Token", exception.getMessage());

                verify(userService, never()).findByEmail(anyString());
                verify(refreshTokenService, never()).delete(any());
            }

            @Test
            @DisplayName("logout should throw InvalidRefreshTokenException when token does not match records")
            void testLogout_TokenDoesNotMatchRecords_ShouldThrowInvalidRefreshTokenException() {
                RefreshToken storedToken = new RefreshToken();
                storedToken.setToken(hashToken("otherToken")); // hash distinto

                when(jwtService.verifyToken("validToken")).thenReturn(true);
                when(jwtService.getEmailFromToken("validToken")).thenReturn(user.getEmail());
                when(userService.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
                when(refreshTokenService.findByUserAndDeviceId(user, "device1")).thenReturn(storedToken);

                InvalidRefreshTokenException exception = assertThrows(InvalidRefreshTokenException.class, () ->
                        authService.logout("validToken", "device1")
                );

                assertEquals("Token does not match records", exception.getMessage());
                verify(refreshTokenService, never()).delete(storedToken);
            }
        }
    }
}
