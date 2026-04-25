package com.expmatik.backend.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.expmatik.backend.chat.DTOs.ChatMessageCreate;
import com.expmatik.backend.chat.DTOs.ChatResponse;
import com.expmatik.backend.jwt.JwtService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private final List<StompSession> activeSessions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(converter);
    }

    @AfterEach
    void tearDown() {
        activeSessions.forEach(session -> {
            if (session.isConnected()) session.disconnect();
        });
        activeSessions.clear();
        stompClient.stop();
    }

    private StompSession connect(String email) throws Exception {
        String token = jwtService.generateAccessTokenFromEmail(email);

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.setOrigin("http://localhost:5173");

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        StompSession session = stompClient.connectAsync(
            "ws://localhost:" + port + "/ws",
            handshakeHeaders,
            connectHeaders,
            new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        activeSessions.add(session);
        return session;
    }

    @Nested
    @DisplayName("sendMessage via WebSocket")
    class SendMessage {

        @Nested
        @DisplayName("Success cases")
        class SuccessCases {

            @Test
            @DisplayName("Should deliver message to both participants when sender is the maintainer")
            void testSendMessage_MaintainerSends_shouldDeliverToBothParticipants() throws Exception {
                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000002");
                BlockingQueue<ChatResponse> adminMessages = new LinkedBlockingQueue<>();
                BlockingQueue<ChatResponse> maintainerMessages = new LinkedBlockingQueue<>();
                String destination = "/user/queue/chat/" + maintenanceId;

                StompSession adminSession = connect("admin@expmatik.com");
                StompSession maintainerSession = connect("repo@expmatik.com");

                adminSession.subscribe(destination, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) { return ChatResponse.class; }
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        adminMessages.offer((ChatResponse) payload);
                    }
                });

                maintainerSession.subscribe(destination, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) { return ChatResponse.class; }
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        maintainerMessages.offer((ChatResponse) payload);
                    }
                });

                Thread.sleep(300);

                maintainerSession.send("/app/chat/" + maintenanceId, new ChatMessageCreate("Hola desde el test"));

                ChatResponse adminReceived = adminMessages.poll(5, TimeUnit.SECONDS);
                ChatResponse maintainerReceived = maintainerMessages.poll(5, TimeUnit.SECONDS);

                assertNotNull(adminReceived, "Admin should receive the message");
                assertNotNull(maintainerReceived, "Maintainer should receive the message");
                assertEquals("Hola desde el test", adminReceived.content());
                assertEquals("Hola desde el test", maintainerReceived.content());
                assertEquals("repo@expmatik.com", adminReceived.sender().email());
                assertEquals(maintenanceId, adminReceived.maintenanceId());
            }

            @Test
            @DisplayName("Should deliver message to both participants when sender is the administrator")
            void testSendMessage_AdministratorSends_shouldDeliverToBothParticipants() throws Exception {
                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000002");
                BlockingQueue<ChatResponse> adminMessages = new LinkedBlockingQueue<>();
                BlockingQueue<ChatResponse> maintainerMessages = new LinkedBlockingQueue<>();
                String destination = "/user/queue/chat/" + maintenanceId;

                StompSession adminSession = connect("admin@expmatik.com");
                StompSession maintainerSession = connect("repo@expmatik.com");

                adminSession.subscribe(destination, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) { return ChatResponse.class; }
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        adminMessages.offer((ChatResponse) payload);
                    }
                });

                maintainerSession.subscribe(destination, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) { return ChatResponse.class; }
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        maintainerMessages.offer((ChatResponse) payload);
                    }
                });

                Thread.sleep(300);

                adminSession.send("/app/chat/" + maintenanceId, new ChatMessageCreate("Hola desde el administrador"));

                ChatResponse adminReceived = adminMessages.poll(5, TimeUnit.SECONDS);
                ChatResponse maintainerReceived = maintainerMessages.poll(5, TimeUnit.SECONDS);

                assertNotNull(adminReceived, "Admin should receive the message");
                assertNotNull(maintainerReceived, "Maintainer should receive the message");
                assertEquals("Hola desde el administrador", adminReceived.content());
                assertEquals("admin@expmatik.com", adminReceived.sender().email());
                assertEquals(maintenanceId, adminReceived.maintenanceId());
            }
        }

        @Nested
        @DisplayName("Failure cases")
        class FailureCases {

            @Test
            @DisplayName("Should reject connection when JWT token is invalid")
            void testSendMessage_InvalidJwt_shouldRejectConnection() {
                WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
                handshakeHeaders.setOrigin("http://localhost:5173");

                StompHeaders connectHeaders = new StompHeaders();
                connectHeaders.add("Authorization", "Bearer invalid.jwt.token");

                CompletableFuture<StompSession> future = stompClient.connectAsync(
                    "ws://localhost:" + port + "/ws",
                    handshakeHeaders,
                    connectHeaders,
                    new StompSessionHandlerAdapter() {}
                );

                assertThrows(Exception.class, () -> future.get(5, TimeUnit.SECONDS));
            }

            @Test
            @DisplayName("Should reject connection when no JWT token is provided")
            void testSendMessage_MissingJwt_shouldRejectConnection() {
                WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
                handshakeHeaders.setOrigin("http://localhost:5173");

                CompletableFuture<StompSession> future = stompClient.connectAsync(
                    "ws://localhost:" + port + "/ws",
                    handshakeHeaders,
                    new StompHeaders(),
                    new StompSessionHandlerAdapter() {}
                );

                assertThrows(Exception.class, () -> future.get(5, TimeUnit.SECONDS));
            }

            @Test
            @DisplayName("Should not deliver message when maintenance is COMPLETED")
            void testSendMessage_MaintenanceCompleted_shouldNotDeliverMessage() throws Exception {
                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                BlockingQueue<ChatResponse> messages = new LinkedBlockingQueue<>();

                StompSession session = connect("repo@expmatik.com");
                session.subscribe("/user/queue/chat/" + maintenanceId, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) { return ChatResponse.class; }
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        messages.offer((ChatResponse) payload);
                    }
                });

                Thread.sleep(300);
                session.send("/app/chat/" + maintenanceId, new ChatMessageCreate("Mensaje a mantenimiento completado"));

                assertNull(messages.poll(2, TimeUnit.SECONDS), "No message should be delivered for a COMPLETED maintenance");
            }

            @Test
            @DisplayName("Should not deliver message when maintenance is REJECTED_EXPIRED")
            void testSendMessage_MaintenanceRejectedExpired_shouldNotDeliverMessage() throws Exception {
                UUID maintenanceId = UUID.fromString("00000000-0000-0000-0000-000000000008");
                BlockingQueue<ChatResponse> messages = new LinkedBlockingQueue<>();

                StompSession session = connect("repo@expmatik.com");
                session.subscribe("/user/queue/chat/" + maintenanceId, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) { return ChatResponse.class; }
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        messages.offer((ChatResponse) payload);
                    }
                });

                Thread.sleep(300);
                session.send("/app/chat/" + maintenanceId, new ChatMessageCreate("Mensaje a mantenimiento expirado"));

                assertNull(messages.poll(2, TimeUnit.SECONDS), "No message should be delivered for a REJECTED_EXPIRED maintenance");
            }
        }
    }
}
