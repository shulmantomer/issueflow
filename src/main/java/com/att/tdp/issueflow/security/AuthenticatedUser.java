package com.att.tdp.issueflow.security;

import com.att.tdp.issueflow.common.enums.Role;
import java.time.Instant;

public record AuthenticatedUser(Long id, String username, Role role, String jti, Instant expiresAt) {}
