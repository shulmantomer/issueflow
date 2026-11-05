package com.shulmantomer.issueflow.project.dto;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 255) String name,
        String description
) {}
