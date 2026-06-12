package com.billing.system.controller;

import com.billing.system.dto.AuthDtos.AuthResponse;
import com.billing.system.dto.AuthDtos.LoginRequest;
import com.billing.system.dto.AuthDtos.MeResponse;
import com.billing.system.dto.AuthDtos.SignupRequest;
import com.billing.system.security.AuthPrincipal;
import com.billing.system.service.AuthService;
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
}
