package com.att.tdp.issueflow.user.dto;

import com.att.tdp.issueflow.common.enums.Role;
import com.att.tdp.issueflow.user.User;

public record UserResponse(Long id, String username, String email, String fullName, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRole());
    }
}
