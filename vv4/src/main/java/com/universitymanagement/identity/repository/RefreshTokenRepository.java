package com.universitymanagement.identity.repository;

import com.universitymanagement.identity.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByRefreshTokenHash(String refreshTokenHash);

    List<RefreshToken> findByUser_IdOrderByLoginTimeDesc(UUID userId);
}
