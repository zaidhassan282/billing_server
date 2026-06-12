package com.billing.system.security;

import com.billing.system.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Issues and validates the JWTs that {@code AuthController.login} hands
 * back. Symmetric HS256: one shared secret, server-side only.
 *
 * Claims:
 *   sub       — user id (numeric, as a string)
 *   email     — login email
 *   tenantId  — numeric tenant id (drives the per-tenant query filter
 *               in Phase 2's TenantInterceptor)
 *   admin     — boolean, true if isTenantAdmin
 *
 * Secret comes from {@code app.jwt.secret} (env var JWT_SECRET in prod).
 * Default is a long string for dev only — DO NOT ship this default to
 * production; bake a real secret via env var.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long ttlMillis;

    public JwtService(
            @Value("${app.jwt.secret:dev-only-change-me-dev-only-change-me-dev-only-change-me-12345}") String secretB64OrPlain,
            @Value("${app.jwt.ttl-seconds:86400}") long ttlSeconds) {
        // Accept either base64 or plain text >=32 chars; both produce the
        // 256-bit key HS256 needs.
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretB64OrPlain);
            if (keyBytes.length < 32) throw new IllegalArgumentException("too short");
        } catch (Exception ignore) {
            keyBytes = secretB64OrPlain.getBytes();
            if (keyBytes.length < 32) {
                throw new IllegalArgumentException(
                        "app.jwt.secret must be at least 32 bytes (256 bits) — current: " + keyBytes.length);
            }
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.ttlMillis = ttlSeconds * 1000L;
    }

    /** Mint a token for the given user. */
    public String issue(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMillis);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("tenantId", user.getTenantId())
                .claim("admin", user.isTenantAdmin())
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parse + verify. Returns the parsed claims on success, throws
     * {@link JwtException} on bad signature / expiry / malformed.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
