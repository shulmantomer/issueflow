package com.shulmantomer.issueflow.user.dto;

import com.shulmantomer.issueflow.common.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotNull Role role
) {}
