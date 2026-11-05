package com.shulmantomer.issueflow.ticket.dependency.dto;

import jakarta.validation.constraints.NotNull;

public record AddDependencyRequest(@NotNull Long blockedBy) {}
