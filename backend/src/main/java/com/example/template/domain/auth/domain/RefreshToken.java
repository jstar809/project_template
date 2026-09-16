package com.example.template.domain.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Refresh Token.
 *
 * <p>원본 문자열은 저장하지 않고 SHA-256 해시만 저장한다. DB가 유출되어도 토큰을 재사용할 수 없게 하기 위해서다.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // 이 엔티티는 수정 이력이 필요 없어 updated_at 없이 created_at만 갖는다(PRD 11.3).
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {}

    private RefreshToken(
            Long userId, String tokenHash, Instant issuedAt, Instant expiresAt, String userAgent, String ipAddress) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = issuedAt;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public static RefreshToken issue(
            Long userId, String tokenHash, Instant issuedAt, Instant expiresAt, String userAgent, String ipAddress) {
        return new RefreshToken(userId, tokenHash, issuedAt, expiresAt, truncate(userAgent), ipAddress);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public boolean isUsable(Instant now) {
        return !isRevoked() && !isExpired(now);
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 255 ? value.substring(0, 255) : value;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
