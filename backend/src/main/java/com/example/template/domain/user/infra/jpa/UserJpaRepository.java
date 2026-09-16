package com.example.template.domain.user.infra.jpa;

import com.example.template.domain.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 저장소. 쓰기와 단건 조회를 담당한다(PRD 8.2). */
public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
