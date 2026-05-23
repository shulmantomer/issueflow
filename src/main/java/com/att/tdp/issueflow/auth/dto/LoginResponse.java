package com.att.tdp.issueflow.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
