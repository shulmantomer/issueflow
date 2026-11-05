package com.shulmantomer.issueflow.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
