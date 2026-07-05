package com.universitymanagement.identity.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "refresh_token_hash", nullable = false)
    private String refreshTokenHash;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "kc_session_id")
    private String kcSessionId;

    @Column(name = "kc_realm")
    private String kcRealm;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @Column(name = "last_refreshed")
    private LocalDateTime lastRefreshed;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "is_active")
    private Boolean isActive;

    private Boolean revoked;

    @Column(name = "revoke_reason")
    private String revokeReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}