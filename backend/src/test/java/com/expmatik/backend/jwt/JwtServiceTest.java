package com.expmatik.backend.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @Spy
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();


        String rawSecret = "12345678901234567890123456789012";
        String secret = Base64.getEncoder().encodeToString(rawSecret.getBytes());

        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "expirationAt", 3600000L); // 1h
        ReflectionTestUtils.setField(jwtService, "expirationRt", 86400000L); // 1 dia
    }

    // == TESTS de generateAccessTokenFromEmail ==

    @Nested
    @DisplayName("Tests for generateAccessTokenFromEmail method")
    class GenerateAccessTokenTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should generate a valid access token")
            void testGenerateAccessTokenFromEmail_ValidEmail_shouldGenerateValidAccessToken() {

                String token = jwtService.generateAccessTokenFromEmail("test@email.com");

                assertNotNull(token);
                assertTrue(jwtService.verifyToken(token));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should throw RuntimeException with correct message when token generation fails")
            void testGenerateAccessTokenFromEmail_InvalidSecretKey_shouldThrowRuntimeException() {

                ReflectionTestUtils.setField(jwtService, "secretKey", "invalidBase64Key");

                RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                    jwtService.generateAccessTokenFromEmail("test@email.com");
                });

                assertEquals("Error generating access token", ex.getMessage());
            }
        }
    }

    // == TESTS de getEmailFromToken ==

    @Nested
    @DisplayName("Tests for getEmailFromToken method")
    class GetEmailFromTokenTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should extract email from token")
            void testGetEmailFromToken_ValidToken_shouldExtractEmail() {

                String token = jwtService.generateAccessTokenFromEmail("test@email.com");

                String email = jwtService.getEmailFromToken(token);

                assertEquals("test@email.com", email);
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should throw exception for invalid token")
            void testGetEmailFromToken_InvalidToken_shouldThrowException() {

                assertThrows(RuntimeException.class, () -> {
                    jwtService.getEmailFromToken("NotAValidToken");
                });
            }
        }
    }

    // == TESTS de getExpirarationFromToken ==

    @Nested
    @DisplayName("Tests for getExpirarationFromToken method")
    class GetExpirationFromTokenTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should return expiration date from token")
            void testGetExpirationFromToken_ValidToken_shouldReturnExpirationDate() {

                String token = jwtService.generateAccessTokenFromEmail("test@email.com");

                assertNotNull(jwtService.getExpirarationFromToken(token));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should throw exception when expiration parsing fails")
            void testGetExpirationFromToken_InvalidToken_shouldThrowException() {

                assertThrows(RuntimeException.class, () -> {
                    jwtService.getExpirarationFromToken("NotAValidToken");
                });
            }
        }
    }

    // == TESTS de generateRefreshTokenFromEmail ==

    @Nested
    @DisplayName("Tests for generateRefreshTokenFromEmail method")
    class GenerateRefreshTokenTests {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should generate a valid refresh token")
            void testGenerateRefreshTokenFromEmail_ValidEmail_shouldGenerateValidRefreshToken() {

                String token = jwtService.generateRefreshTokenFromEmail("test@email.com");

                assertNotNull(token);
                assertTrue(jwtService.verifyToken(token));
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should throw RuntimeException when generating refresh token fails")
            void testGenerateRefreshTokenFromEmail_InvalidSecretKey_shouldThrowRuntimeException() {

                ReflectionTestUtils.setField(jwtService, "secretKey", "invalidBase64Key");

                RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                    jwtService.generateRefreshTokenFromEmail("test@email.com");
                });
                assertEquals("Error generating refresh token", ex.getMessage());
            }
        }
    }

    // == TESTS de verifyToken ==

    @Nested
    @DisplayName("Tests for verifyToken method")
    class VerifyTokenTests {

        @Nested
        @DisplayName("Valid token cases")
        class ValidTokenCases {

            @Test
            @DisplayName("Should return true for valid token")
            void testVerifyToken_ValidToken_shouldReturnTrue() {

                ReflectionTestUtils.setField(jwtService, "expirationAt", 3600000L);

                String token = jwtService.generateAccessTokenFromEmail("test@email.com");

                boolean result = jwtService.verifyToken(token);

                assertTrue(result);
            }
        }

        @Nested
        @DisplayName("Invalid token cases")
        class InvalidTokenCases {

            @Test
            @DisplayName("Should return false for malformed token")
            void testVerifyToken_MalformedToken_shouldReturnFalse() {

                boolean result = jwtService.verifyToken("NotAValidToken");

                assertFalse(result);
            }

            @Test
            @DisplayName("Should return false for expired token")
            void testVerifyToken_ExpiredToken_shouldReturnFalse() throws InterruptedException {

                ReflectionTestUtils.setField(jwtService, "expirationAt", 1L);

                String token = jwtService.generateAccessTokenFromEmail("test@email.com");

                Thread.sleep(5);

                boolean result = jwtService.verifyToken(token);

                assertFalse(result);
            }

            @Test
            @DisplayName("Should return false for token signed with different key")
            void testVerifyToken_DifferentKey_shouldReturnFalse() {

                JwtService anotherService = new JwtService();

                String rawSecret = "DifferentSecretKey12345678901234567890";
                String secret = Base64.getEncoder().encodeToString(rawSecret.getBytes());

                ReflectionTestUtils.setField(anotherService, "secretKey", secret);
                ReflectionTestUtils.setField(anotherService, "expirationAt", 3600000L);

                String token = anotherService.generateAccessTokenFromEmail("test@email.com");

                boolean result = jwtService.verifyToken(token);

                assertFalse(result);
            }

            @Test
            @DisplayName("Should return false for null token")
            void testVerifyToken_NullToken_shouldReturnFalse() {

                boolean result = jwtService.verifyToken(null);

                assertFalse(result);
            }
        }
    }
}
