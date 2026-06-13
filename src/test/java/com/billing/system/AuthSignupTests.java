package com.billing.system;

import com.billing.system.dto.AuthDtos.AuthResponse;
import com.billing.system.dto.AuthDtos.SignupRequest;
import com.billing.system.entity.User;
import com.billing.system.repository.UserRepository;
import com.billing.system.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the exact signup path that's been blowing up in the field
 * with "Transaction silently rolled back". If this passes, the bug is
 * deploy/cache; if it fails, the stack trace will surface the cause.
 */
@SpringBootTest
class AuthSignupTests {

    @Autowired AuthService authService;
    @Autowired UserRepository users;

    @Test
    void signup_creates_tenant_and_admin_and_returns_token() {
        SignupRequest req = new SignupRequest(
                "isolation@example.com",
                "demo12345",
                "Iso Tester",
                "Iso Testing Co"
        );
        AuthResponse resp = authService.signup(req);

        assertNotNull(resp, "Response must not be null");
        assertNotNull(resp.token(), "Should mint a JWT");
        assertTrue(resp.admin(), "Signup user must be tenant admin");
        assertNotNull(resp.tenantId(), "Tenant must be created");
        assertEquals("Iso Testing Co", resp.tenantName());

        User stored = users.findByEmail("isolation@example.com").orElseThrow();
        assertEquals(resp.tenantId(), stored.getTenantId(),
                "Stored user must belong to the new tenant");
        assertTrue(stored.isTenantAdmin(), "Stored user must be admin");
    }
}
