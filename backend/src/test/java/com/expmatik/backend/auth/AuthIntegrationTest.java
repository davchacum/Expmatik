package com.expmatik.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.expmatik.backend.auth.DTOs.AuthRequest;
import com.expmatik.backend.auth.DTOs.AuthRequestLogin;
import com.expmatik.backend.auth.DTOs.RefreshTokenRequest;
import com.expmatik.backend.user.Role;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        // == Test de POST /api/auth/register ==

        @Nested
        @DisplayName("POST /api/auth/register")
        class RegisterTests {

                @Nested
                @DisplayName("Success cases")
                class SuccessCases {


                        @Test
                        void testRegister_validRequest_shouldCreateUserAndReturnToken() throws Exception {

                                AuthRequest request = new AuthRequest(
                                        "test@email.com",
                                        "password123",
                                        "device1",
                                        Role.ADMINISTRATOR,
                                        "John",
                                        "Doe"
                                );

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.token").exists())
                                        .andExpect(cookie().exists("refresh_token"));
                        }
                }

                @Nested
                @DisplayName("Failure cases")
                class FailureCases {

                        @Test
                        void testRegister_UserExists_shouldThrowConflictException() throws Exception {

                                AuthRequest request = new AuthRequest(
                                        "duplicate@email.com",
                                        "password",
                                        "device1",
                                        Role.ADMINISTRATOR,
                                        "John",
                                        "Doe"
                                );
                                //Primero registramos el usuario
                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                                //Intentamos registrarlo de nuevo, lo que debería fallar
                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isConflict());
                        }
                }
        }

        //== Test de POST /api/auth/login ==

        @Nested
        @DisplayName("POST /api/auth/login")
        class LoginTests {

                @Nested
                @DisplayName("Success cases")
                class SuccessCases {

                        @Test
                        void testLogin_validCredentials_shouldReturnAccessToken() throws Exception {

                                // primero registrar
                                AuthRequest register = new AuthRequest(
                                        "login@email.com",
                                        "password",
                                        "device1",
                                        Role.ADMINISTRATOR,
                                        "John",
                                        "Doe"
                                );

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(register)));

                                AuthRequestLogin login = new AuthRequestLogin(
                                        "login@email.com",
                                        "password",
                                        "device1"
                                );

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(login)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.token").exists())
                                        .andExpect(cookie().exists("refresh_token"));
                        }

                        @Test
                        void testLogin_RefreshTokenCookieSetCorrectly_shouldSucceed() throws Exception {

                                AuthRequest register = new AuthRequest(
                                        "cookie@email.com",
                                        "password",
                                        "device1",
                                        Role.ADMINISTRATOR,
                                        "John",
                                        "Doe"
                                );

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(register)));

                                AuthRequestLogin login = new AuthRequestLogin(
                                        "cookie@email.com",
                                        "password",
                                        "device1"
                                );

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(login)))
                                        .andExpect(status().isOk())
                                        .andExpect(cookie().exists("refresh_token"))
                                        .andExpect(cookie().httpOnly("refresh_token", true))
                                        .andExpect(cookie().path("refresh_token", "/"));
                        }
                }

                @Nested
                @DisplayName("Failure cases")
                class FailureCases {

                        @Test
                        void testLogin_WrongPassword_shouldThrowUnauthorizedException() throws Exception {

                                // primero registrar
                                AuthRequest register = new AuthRequest(
                                        "login@email.com",
                                        "password",
                                        "device1",
                                        Role.ADMINISTRATOR,
                                        "John",
                                        "Doe"
                                );

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(register)));

                                AuthRequestLogin login = new AuthRequestLogin(
                                        "login@email.com",
                                        "wrongpassword",
                                        "device1"
                                );

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(login)))
                                        .andExpect(status().isUnauthorized());
                        }

                        @Test
                        void testLogin_UnregisteredUser_shouldThrowResourceNotFoundException() throws Exception {

                                AuthRequestLogin login = new AuthRequestLogin(
                                        "unregistered@email.com",
                                        "password",
                                        "device1"
                                );

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(login)))
                                        .andExpect(status().isNotFound());
                        }

                        @Test
                        void testLogin_InvalidRequest_shouldThrowBadRequestException() throws Exception {

                                AuthRequestLogin login = new AuthRequestLogin(
                                        "",
                                        "",
                                        "device1"
                                );

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(login)))
                                        .andExpect(status().isBadRequest());
                        }
                }
        }



        //== Test de POST /api/auth/refresh ==
        @Nested
        @DisplayName("POST /api/auth/refresh")
        class RefreshTests {

                @Nested
                @DisplayName("Success cases")
                class SuccessCases {

                        @Test
                        void testRefresh_validRequest_shouldReturnNewAccessToken() throws Exception {

                                AuthRequest register = new AuthRequest(
                                        "refresh@email.com",
                                        "password",
                                        "device1",
                                        Role.ADMINISTRATOR,
                                        "John",
                                        "Doe"
                                );

                                MvcResult registerResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(register)))
                                        .andExpect(status().isOk())
                                        .andReturn();

                                Cookie refreshCookie = registerResult.getResponse().getCookie("refresh_token");

                                RefreshTokenRequest request = new RefreshTokenRequest("device1");

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/refresh")
                                                .cookie(refreshCookie)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.token").exists())
                                        .andExpect(cookie().exists("refresh_token"));
                        }
                }

                @Nested
                @DisplayName("Failure cases")
                class FailureCases {

                        @Test
                        void testRefresh_CookieMissing_shouldThrowBadRequestException() throws Exception {

                                RefreshTokenRequest request = new RefreshTokenRequest("device1");

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/refresh")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());
                        }

                        @Test
                        void testRefresh_TokenInvalid_shouldThrowUnauthorizedException() throws Exception {

                                Cookie fakeCookie = new Cookie("refresh_token", "invalidToken");

                                RefreshTokenRequest request = new RefreshTokenRequest("device1");

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/refresh")
                                                .cookie(fakeCookie)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isUnauthorized());
                        }

                        @Test
                        void testRefresh_DeviceIdMismatch_shouldThrowUnauthorizedException() throws Exception {

                                AuthRequest register = new AuthRequest(
                                        "device@test.com",
                                        "password",
                                        "device1",
                                        Role.ADMINISTRATOR,
                                        "John",
                                        "Doe"
                                );

                                MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(register)))
                                        .andReturn();

                                Cookie refreshCookie = result.getResponse().getCookie("refresh_token");

                                RefreshTokenRequest request = new RefreshTokenRequest("wrongDevice");

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/refresh")
                                                .cookie(refreshCookie)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isUnauthorized());
                        }
                }
        }

    // == Test de POST /api/auth/logout ==

        @Nested
        @DisplayName("POST /api/auth/logout")
        class LogoutTests {

                @Nested
                @DisplayName("Success cases")
                class SuccessCases {

                        @Test
                        void testLogout_shouldClearRefreshTokenCookie_shouldWork() throws Exception {

                                //Registrar usuario y obtener cookie refresh
                                AuthRequest register = new AuthRequest(
                                        "logout@email.com",
                                        "password",
                                        "device1",
                                        Role.ADMINISTRATOR,
                                        "Jane",
                                        "Doe"
                                );

                                MvcResult registerResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(register)))
                                .andExpect(status().isOk())
                                .andReturn();

                                Cookie refreshCookie = registerResult.getResponse().getCookie("refresh_token");
                                assertNotNull(refreshCookie);

                                // Obtener el access token del registro
                                String responseBody = registerResult.getResponse().getContentAsString();

                                String accessToken = objectMapper.readTree(responseBody).get("token").asText();

                                //Preparar request logout
                                RefreshTokenRequest request = new RefreshTokenRequest("device1");

                                //Llamada al endpoint /logout con Authorization
                                MvcResult logoutResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout")
                                        .cookie(refreshCookie)
                                        .header("Authorization", "Bearer " + accessToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                                //Verificar que la cookie se ha borrado
                                Cookie deletedCookie = logoutResult.getResponse().getCookie("refresh_token");
                                assertNotNull(deletedCookie);
                                assertEquals(0, deletedCookie.getMaxAge(), "La cookie debe eliminarse (MaxAge = 0)");
                                assertNull(deletedCookie.getValue(), "El valor de la cookie debe ser nulo tras logout");
                        }
                }

                @Nested
                @DisplayName("Failure cases")
                class FailureCases {

                        @Test
                        void testLogout_InvalidRefreshToken_shouldThrowUnauthorizedException() throws Exception {

                                // Creamos una cookie con un token falso
                                Cookie fakeCookie = new Cookie("refresh_token", "invalidToken");

                                // DeviceId cualquiera
                                RefreshTokenRequest request = new RefreshTokenRequest("device1");

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout")
                                                .cookie(fakeCookie)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isUnauthorized());
                        }

                        @Test
                        void testLogout_DeviceIdMismatch_shouldThrowUnauthorizedException() throws Exception {

                                //Registrar usuario y obtener refresh token real
                                AuthRequest register = new AuthRequest(
                                        "logout2@email.com",
                                        "password",
                                        "device1",
                                        Role.ADMINISTRATOR,
                                        "Jane",
                                        "Doe"
                                );

                                MvcResult registerResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(register)))
                                        .andExpect(status().isOk())
                                        .andReturn();

                                Cookie refreshCookie = registerResult.getResponse().getCookie("refresh_token");
                                assertNotNull(refreshCookie);

                                //Hacemos logout con deviceId incorrecto
                                RefreshTokenRequest wrongDeviceRequest = new RefreshTokenRequest("wrongDevice");

                                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout")
                                                .cookie(refreshCookie)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(wrongDeviceRequest)))
                                        .andExpect(status().isUnauthorized());
                        }
                }
        }

        // == Test de GET /api/auth/validate ==

        @Nested
        @DisplayName("GET /api/auth/validate")
        class ValidateTokenTests {
                @Nested
                @DisplayName("Success cases")
                class SuccessCases {

                                                @Test
                        void testValidateToken_ValidToken_shouldReturnValid() throws Exception {

                                //Registrar usuario y obtener access token
                                AuthRequest register = new AuthRequest(
                                        "validate@email.com",
                                        "password",
                                        "device1",
                                        Role.ADMINISTRATOR,
                                        "Alice",
                                        "Smith"
                                );

                                MvcResult registerResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(register)))
                                        .andExpect(status().isOk())
                                        .andReturn();

                                String responseBody = registerResult.getResponse().getContentAsString();
                                String accessToken = objectMapper.readTree(responseBody).get("token").asText();

                                //Validar el access token
                                mockMvc.perform(get("/api/auth/validate")
                                                .header("Authorization", "Bearer " + accessToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.authenticated").value(true))
                                        .andExpect(jsonPath("$.message").value("Token valid"));
                        }
                }

                @Nested
                @DisplayName("Failure cases")
                class FailureCases {

                        @Test
                        void testValidateToken_MissingHeader_shouldReturnBadRequestException() throws Exception {
                                mockMvc.perform(get("/api/auth/validate"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.authenticated").value(false))
                                        .andExpect(jsonPath("$.message").value("Missing token"));
                        }

                        @Test
                        void testValidateToken_HeaderMalformed_shouldReturnBadRequestException() throws Exception {
                                mockMvc.perform(get("/api/auth/validate")
                                                .header("Authorization", "Token xyz"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.authenticated").value(false))
                                        .andExpect(jsonPath("$.message").value("Missing token"));
                        }

                        @Test
                        void testValidateToken_InvalidToken_shouldReturnInvalidToken() throws Exception {
                                String fakeToken = "this.is.not.valid";

                                mockMvc.perform(get("/api/auth/validate")
                                                .header("Authorization", "Bearer " + fakeToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.authenticated").value(false))
                                        .andExpect(jsonPath("$.message").value("Invalid or expired token"));
                        }
                }
        }



    // == Test de /api/auth/profile ==
        @Nested
        @DisplayName("GET /api/auth/profile")
        class GetProfileTests {

                @Nested
                @DisplayName("Success cases")
                class SuccessCases {
                        
                        @Test
                        void testGetProfile_AuthenticatedUser_shouldReturnUserProfile() throws Exception {

                                // Registrar usuario y obtener access token
                                AuthRequest register = new AuthRequest(
                                        "profile@email.com",
                                        "password",
                                        "device1",
                                        Role.ADMINISTRATOR,
                                        "Alice",
                                        "Smith"
                                );

                                MvcResult registerResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(register)))
                                        .andExpect(status().isOk())
                                        .andReturn();

                                String responseBody = registerResult.getResponse().getContentAsString();
                                String accessToken = objectMapper.readTree(responseBody).get("token").asText();

                                // Obtener el perfil del usuario autenticado
                                mockMvc.perform(get("/api/auth/profile")
                                                .header("Authorization", "Bearer " + accessToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.email").value("profile@email.com"))
                                        .andExpect(jsonPath("$.firstName").value("Alice"))
                                        .andExpect(jsonPath("$.lastName").value("Smith"))
                                        .andExpect(jsonPath("$.role").value("ADMINISTRATOR"));
                        }
                }

                @Nested
                @DisplayName("Failure cases")
                class FailureCases {
                                
                        @Test
                        void testGetProfile_NoToken_shouldReturnUnauthorizedException() throws Exception {
                                mockMvc.perform(get("/api/auth/profile"))
                                        .andExpect(status().isUnauthorized());
                        }

                        @Test
                        void testGetProfile_InvalidToken_shouldReturnUnauthorizedException() throws Exception {
                                String fakeToken = "this.is.not.valid";
                                mockMvc.perform(get("/api/auth/profile")
                                                .header("Authorization", "Bearer " + fakeToken))
                                        .andExpect(status().isUnauthorized());
                        }
                }
        }
}
