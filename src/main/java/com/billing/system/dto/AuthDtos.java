package com.billing.system.dto;

/**
 * Request and response shapes for /auth/login, /auth/signup, /auth/me.
 * Bundled into one file because each is a trivial record and keeping
 * them together makes the contract easy to scan.
 */
public class AuthDtos {

    /** POST /auth/login. */
    public record LoginRequest(String email, String password) { }

    /** POST /auth/signup. */
    public record SignupRequest(String email,
                                String password,
                                String displayName,
                                String companyName) { }

    /** Both /auth/login and /auth/signup return this. */
    public record AuthResponse(String token,
                               Long userId,
                               String email,
                               Long tenantId,
                               String tenantName,
                               boolean admin) { }

    /** GET /auth/me — diagnostic, confirms the JWT pipeline works. */
    public record MeResponse(Long userId,
                             String email,
                             Long tenantId,
                             boolean admin) { }

    /** GET /auth/verify?token=... result. */
    public record VerifyResponse(String email, boolean verified, String message) { }
}
