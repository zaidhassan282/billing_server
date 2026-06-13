package com.billing.system.repository;

import com.billing.system.entity.VerifyToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerifyTokenRepository extends JpaRepository<VerifyToken, Long> {

    Optional<VerifyToken> findByToken(String token);
}
