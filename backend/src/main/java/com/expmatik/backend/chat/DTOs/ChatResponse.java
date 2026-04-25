package com.expmatik.backend.chat.DTOs;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.expmatik.backend.chat.Chat;
import com.expmatik.backend.user.DTOs.UserProfile;

public record ChatResponse(

    UUID id,

    String content,

    LocalDateTime sentAt,

    LocalDateTime readAt,

    UUID maintenanceId,

    UserProfile sender

) {

    public static ChatResponse fromChat(Chat chat) {
        return new ChatResponse(
            chat.getId(),
            chat.getContent(),
            chat.getSentAt(),
            chat.getReadAt(),
            chat.getMaintenance().getId(),
            UserProfile.fromUser(chat.getSender())
        );
    }

    public static List<ChatResponse> fromChatList(List<Chat> chats) {
        return chats.stream().map(ChatResponse::fromChat).toList();
    }

}
