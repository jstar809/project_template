package com.example.template.domain.user.domain;

import java.util.Optional;

/**
 * 사용자 저장소 포트.
 *
 * <p>domain 계층은 이 인터페이스만 알고, 실제 구현(JPA)은 infra 계층에 둔다(PRD ARCH-01).
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
