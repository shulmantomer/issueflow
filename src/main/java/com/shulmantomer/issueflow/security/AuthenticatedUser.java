package com.shulmantomer.issueflow.security;

import com.shulmantomer.issueflow.common.enums.Role;
import java.time.Instant;

public record AuthenticatedUser(Long id, String username, Role role, String jti, Instant expiresAt) {}
