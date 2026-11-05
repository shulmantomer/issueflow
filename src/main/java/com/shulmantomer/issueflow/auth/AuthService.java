package com.shulmantomer.issueflow.auth;

import com.shulmantomer.issueflow.auth.dto.LoginRequest;
import com.shulmantomer.issueflow.auth.dto.LoginResponse;
import com.shulmantomer.issueflow.exception.NotFoundException;
import com.shulmantomer.issueflow.exception.UnauthorizedException;
import com.shulmantomer.issueflow.security.AuthenticatedUser;
import com.shulmantomer.issueflow.security.JwtService;
import com.shulmantomer.issueflow.security.TokenDenyListEntry;
import com.shulmantomer.issueflow.security.TokenDenyListRepository;
import com.shulmantomer.issueflow.user.User;
import com.shulmantomer.issueflow.user.UserRepository;
import com.shulmantomer.issueflow.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication operations — login (issues a JWT), logout (deny-lists the
 * current jti until natural expiry), and {@code /auth/me} profile lookup.
 * Login returns the same generic error for missing user and bad password to
 * avoid username enumeration.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenDenyListRepository denyListRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       TokenDenyListRepository denyListRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.denyListRepository = denyListRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Same message for missing user and bad password — avoids username enumeration.
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        JwtService.IssuedToken issued = jwtService.generate(user);
        return new LoginResponse(issued.token(), "Bearer", jwtService.getExpiresInSeconds());
    }

    @Transactional
    public void logout(AuthenticatedUser principal) {
        if (principal != null && !denyListRepository.existsById(principal.jti())) {
            denyListRepository.save(new TokenDenyListEntry(principal.jti(), principal.expiresAt()));
        }
    }

    @Transactional(readOnly = true)
    public UserResponse me(AuthenticatedUser principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("Authenticated user no longer exists"));
        return UserResponse.from(user);
    }
}
