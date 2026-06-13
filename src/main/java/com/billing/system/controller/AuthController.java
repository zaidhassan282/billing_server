package com.billing.system.controller;

import com.billing.system.dto.AuthDtos.AuthResponse;
import com.billing.system.dto.AuthDtos.LoginRequest;
import com.billing.system.dto.AuthDtos.MeResponse;
import com.billing.system.dto.AuthDtos.SignupRequest;
import com.billing.system.dto.AuthDtos.VerifyResponse;
import com.billing.system.security.AuthPrincipal;
import com.billing.system.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Public auth endpoints.
 *
 * /auth/login   — POST, body {email, password} → token + user info
 * /auth/signup  — POST, body {email, password, displayName, companyName}
 *                  → creates a fresh tenant + admin user, returns token
 * /auth/me      — GET, requires Bearer token → echoes the principal
 *                  (diagnostic for confirming the JWT pipeline works)
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/signup")
    public AuthResponse signup(@RequestBody SignupRequest req) {
        return auth.signup(req);
    }

    @GetMapping("/me")
    public MeResponse me() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthPrincipal p)) {
            throw new RuntimeException("Not authenticated");
        }
        return new MeResponse(p.userId(), p.email(), p.tenantId(), p.admin());
    }

    /**
     * Email-verification landing. Phase 2 stub: link is logged on
     * signup, user clicks it, server marks the account verified. P4-1
     * swaps the log line for a real SMTP send. Idempotent on a token
     * that's already been used.
     */
    @GetMapping("/verify")
    public VerifyResponse verify(@RequestParam(value = "token", required = false) String token) {
        return auth.verify(token);
    }

    /**
     * Stateless logout — there's no server session to invalidate;
     * the frontend just drops its stored JWT. This endpoint is here so
     * the SPA can hit it on logout for audit logging once user-level
     * auditing lands. For now it's a no-op 204.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
