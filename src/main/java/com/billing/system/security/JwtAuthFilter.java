package com.billing.system.security;

import com.billing.system.entity.User;
import com.billing.system.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Per-request JWT auth filter. Runs before Spring Security's
 * UsernamePasswordAuthenticationFilter; if the Authorization header
 * carries a valid Bearer token, the SecurityContext is populated with
 * an authenticated principal carrying the tenant id.
 *
 * No header / malformed / expired → just doesn't authenticate; the rest
 * of the chain decides whether the endpoint needs auth.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIX.length()).trim();
            try {
                Claims claims = jwt.parse(token);
                Long userId = Long.parseLong(claims.getSubject());
                Optional<User> userOpt = users.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    var authorities = List.of(
                            new SimpleGrantedAuthority(user.isTenantAdmin() ? "ROLE_ADMIN" : "ROLE_USER"));
                    var auth = new UsernamePasswordAuthenticationToken(
                            new AuthPrincipal(user.getId(), user.getEmail(), user.getTenantId(), user.isTenantAdmin()),
                            null,
                            authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // Invalid token → leave context unauthenticated; the
                // request goes on and either hits a permitAll endpoint
                // or gets a 401 from the filter chain.
            }
        }
        chain.doFilter(req, res);
    }
}
