package com.example.template.domain.user.infra.jpa;

import com.example.template.domain.user.domain.User;
import com.example.template.domain.user.domain.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** domain의 포트를 JPA 구현과 연결한다. */
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        return jpaRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(User.normalizeEmail(email));
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(User.normalizeEmail(email));
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return jpaRepository.existsByNickname(nickname);
    }
}
