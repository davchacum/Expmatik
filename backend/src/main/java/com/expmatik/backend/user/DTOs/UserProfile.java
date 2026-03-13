package com.expmatik.backend.user.DTOs;

import com.expmatik.backend.user.Role;
import com.expmatik.backend.user.User;

public record UserProfile(
    String email,
    String firstName,
    String lastName,
    Role role
) {
    public static UserProfile fromUser ( User user ) {
        return new UserProfile(
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole()
        );
    }

}
