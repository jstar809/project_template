package com.example.template.domain.user.domain;

import com.example.template.global.common.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

/**
 * 사용자.
 *
 * <p>setter를 두지 않고 의미 있는 메서드로만 상태를 바꾼다(PRD 컨벤션).
 */
@Entity
@Table(name = "users")
public class User extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected User() {}

    private User(String email, String passwordHash, String nickname, Role role) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    public static User createUser(String email, String passwordHash, String nickname) {
        return new User(email, passwordHash, nickname, Role.USER);
    }

    public static User createAdmin(String email, String passwordHash, String nickname) {
        return new User(email, passwordHash, nickname, Role.ADMIN);
    }

    /** 이메일은 대소문자를 구분하지 않도록 소문자로 정규화한다. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void recordLogin(Instant now) {
        this.lastLoginAt = now;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE && !isDeleted();
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
