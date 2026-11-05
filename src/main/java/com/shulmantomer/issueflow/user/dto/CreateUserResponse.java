package com.shulmantomer.issueflow.user.dto;

import com.shulmantomer.issueflow.common.enums.Role;
import com.shulmantomer.issueflow.user.User;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateUserResponse(
        Long id, String username, String email, String fullName, Role role,
        String generatedPassword
) {
    public static CreateUserResponse from(User user, String generatedPassword) {
        return new CreateUserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRole(), generatedPassword);
    }
}
