package com.billing.system.service;

import com.billing.system.dto.AuthDtos.AuthResponse;
import com.billing.system.dto.AuthDtos.LoginRequest;
import com.billing.system.dto.AuthDtos.SignupRequest;
import com.billing.system.entity.Tenant;
import com.billing.system.entity.User;
import com.billing.system.repository.TenantRepository;
import com.billing.system.repository.UserRepository;
import com.billing.system.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final UserRepository users;
    private final TenantRepository tenants;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;

    public AuthService(UserRepository users,
                       TenantRepository tenants,
                       PasswordEncoder encoder,
                       JwtService jwt,
                       AuditService audit) {
        this.users = users;
        this.tenants = tenants;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
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

        audit.logCreate("User", String.valueOf(savedUser.getId()), savedUser.getEmail(),
                savedUser, "Signup — tenant " + savedTenant.getName() + " created");

        return respond(savedUser);
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
