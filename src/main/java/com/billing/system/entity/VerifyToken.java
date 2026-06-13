package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Email-verification token issued on signup. Phase 2's email verify is
 * still a stub — the link is printed to the backend console — but the
 * mechanism is real so swapping in SMTP (P4-1) is a config change, not
 * a rewrite.
 *
 * Token is a UUID, single-use, expires in 24 hours.
 */
@Getter
@Setter
@Entity
@Table(name = "verify_token",
       indexes = {
           @Index(name = "ix_verify_token_token", columnList = "token", unique = true),
           @Index(name = "ix_verify_token_user", columnList = "user_id"),
       })
public class VerifyToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;
}
