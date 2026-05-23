package com.att.tdp.issueflow.security;

import com.att.tdp.issueflow.common.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-request filter: extracts the {@code Bearer} token, validates its
 * signature and expiry, rejects deny-listed jtis, and populates the
 * {@code SecurityContextHolder} with an {@link AuthenticatedUser} principal.
 * On any failure it leaves the context empty so the entry point produces 401.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenDenyListRepository denyListRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   TokenDenyListRepository denyListRepository) {
        this.jwtService = jwtService;
        this.denyListRepository = denyListRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = header.substring(BEARER_PREFIX.length());
                Jws<Claims> jws = jwtService.parse(token);
                Claims claims = jws.getPayload();
                String jti = claims.getId();

                if (jti != null && !denyListRepository.existsById(jti)) {
                    AuthenticatedUser principal = new AuthenticatedUser(
                            claims.get("uid", Long.class),
                            claims.getSubject(),
                            Role.valueOf(claims.get("role", String.class)),
                            jti,
                            claims.getExpiration().toInstant());

                    var authToken = new UsernamePasswordAuthenticationToken(
                            principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ignored) {
                // Invalid/expired/tampered token -> leave context empty; entry point returns 401.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
