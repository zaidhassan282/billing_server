package com.billing.system.service;

import com.billing.system.dto.AuthDtos.AuthResponse;
import com.billing.system.dto.AuthDtos.LoginRequest;
import com.billing.system.dto.AuthDtos.SignupRequest;
import com.billing.system.dto.AuthDtos.VerifyResponse;
import com.billing.system.entity.Tenant;
import com.billing.system.entity.User;
import com.billing.system.entity.VerifyToken;
import com.billing.system.repository.TenantRepository;
import com.billing.system.repository.UserRepository;
import com.billing.system.repository.VerifyTokenRepository;
import com.billing.system.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Login + signup business logic. Both paths end with a JWT issued by
 * {@link JwtService}.
 *
 * Signup is "public" — Phase 4 will gate it behind email verification;
 * for now it creates the tenant + admin user atomically and lets the
 * client log in straight away. Suitable for the development phase.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final TenantRepository tenants;
    private final VerifyTokenRepository verifyTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final String appBaseUrl;

    public AuthService(UserRepository users,
                       TenantRepository tenants,
                       VerifyTokenRepository verifyTokens,
                       PasswordEncoder encoder,
                       JwtService jwt,
                       AuditService audit,
                       @Value("${app.base-url:http://localhost:8080}") String appBaseUrl) {
        this.users = users;
        this.tenants = tenants;
        this.verifyTokens = verifyTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.appBaseUrl = appBaseUrl;
    }

    public AuthResponse login(LoginRequest req) {
        if (req == null || isBlank(req.email()) || isBlank(req.password())) {
            throw new RuntimeException("Email and password are required");
        }
        User user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }
        return respond(user);
    }

    @Transactional
    public AuthResponse signup(SignupRequest req) {
        if (req == null
                || isBlank(req.email())
                || isBlank(req.password())
                || isBlank(req.companyName())) {
            throw new RuntimeException("Email, password and company name are required");
        }
        if (req.password().length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new RuntimeException("That email is already registered");
        }

        // Each public signup creates its OWN tenant. The first user of a
        // tenant is automatically its admin.
        Tenant tenant = new Tenant();
        tenant.setName(req.companyName().trim());
        tenant.setGstRate(0.18);
        tenant.setCurrency("PKR");
        tenant.setTermsAndConditions(
                "1. Goods once sold will not be returned without prior approval.\n"
                + "2. Payment due within agreed terms.");
        Tenant savedTenant = tenants.save(tenant);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setTenantId(savedTenant.getId());
        user.setTenantAdmin(true);
        user.setDisplayName(req.displayName() == null ? email : req.displayName());
        // Phase 4 will require email verification before activation; for
        // now we mark new accounts verified so the dev flow is unblocked.
        user.setEmailVerified(true);
        User savedUser = users.save(user);

        // P2-7 stub: issue a verify token + log the link. P4-1 swaps the
        // log line for a real SMTP send.
        issueAndLogVerifyLink(savedUser);

        audit.logCreate("User", String.valueOf(savedUser.getId()), savedUser.getEmail(),
                savedUser, "Signup — tenant " + savedTenant.getName() + " created");

        return respond(savedUser);
    }

    /**
     * Verify (idempotent): looks up the token, marks it used, marks the
     * user as verified. Returns a small descriptive payload.
     */
    @Transactional
    public VerifyResponse verify(String tokenStr) {
        if (tokenStr == null || tokenStr.isBlank()) {
            return new VerifyResponse(null, false, "Missing token");
        }
        VerifyToken token = verifyTokens.findByToken(tokenStr).orElse(null);
        if (token == null) {
            return new VerifyResponse(null, false, "Token not found");
        }
        if (token.isUsed()) {
            User u = users.findById(token.getUserId()).orElse(null);
            return new VerifyResponse(u == null ? null : u.getEmail(), true,
                    "Already verified");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return new VerifyResponse(null, false, "Token expired");
        }
        token.setUsed(true);
        verifyTokens.save(token);
        User user = users.findById(token.getUserId()).orElse(null);
        if (user != null && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            users.save(user);
        }
        return new VerifyResponse(user == null ? null : user.getEmail(), true,
                "Email verified");
    }

    private void issueAndLogVerifyLink(User user) {
        VerifyToken vt = new VerifyToken();
        vt.setToken(UUID.randomUUID().toString().replace("-", ""));
        vt.setUserId(user.getId());
        vt.setExpiresAt(LocalDateTime.now().plusHours(24));
        verifyTokens.save(vt);
        String link = appBaseUrl + "/auth/verify?token=" + vt.getToken();
        log.warn("==== Email verify link for {} ====\n  {}\n=================================",
                user.getEmail(), link);
    }

    private AuthResponse respond(User user) {
        String tenantName = tenants.findById(user.getTenantId())
                .map(Tenant::getName)
                .orElse(null);
        return new AuthResponse(
                jwt.issue(user),
                user.getId(),
                user.getEmail(),
                user.getTenantId(),
                tenantName,
                user.isTenantAdmin());
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
