package com.expmatik.backend.user.DTOs;

import java.util.List;
import java.util.UUID;

import com.expmatik.backend.user.User;

public record UserDTO(UUID id, String email, String role, String firstName, String lastName) {

    public static UserDTO fromUser(User user) {
        return new UserDTO(user.getId(), user.getEmail(), user.getRole().name(), user.getFirstName(), user.getLastName());
    }

    public static List<UserDTO> fromUserList(List<User> users) {

        return users.stream()
                .map(UserDTO::fromUser)
                .toList();
    }
}
