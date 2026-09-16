package com.example.template.domain.user.application;

import com.example.template.domain.user.domain.Role;
import com.example.template.domain.user.domain.User;
import com.example.template.domain.user.domain.UserErrorCode;
import com.example.template.domain.user.domain.UserRepository;
import com.example.template.global.error.BusinessException;
import java.time.Clock;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 업무 로직.
 *
 * <p>다른 도메인(auth)은 이 서비스를 통해서만 사용자에 접근한다(PRD ARCH-04).
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** 회원가입. 이메일과 닉네임 중복을 확인한다. */
    @Transactional
    public User register(String email, String rawPassword, String nickname) {
        validateDuplicated(email, nickname);
        User user = User.createUser(email, passwordEncoder.encode(rawPassword), nickname);
        return userRepository.save(user);
    }

    /** 관리자 계정을 만든다. 초기화 로직에서 사용한다. */
    @Transactional
    public User registerAdmin(String email, String rawPassword, String nickname) {
        validateDuplicated(email, nickname);
        User admin = User.createAdmin(email, passwordEncoder.encode(rawPassword), nickname);
        return userRepository.save(admin);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    /** 비밀번호가 맞는지 확인한다. */
    public boolean matchesPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    @Transactional
    public void recordLogin(User user) {
        // 조회된 엔티티를 다시 붙여 변경 감지로 갱신한다.
        userRepository.findById(user.getId()).ifPresent(found -> found.recordLogin(clock.instant()));
    }

    @Transactional
    public User changeNickname(Long userId, String nickname) {
        User user = getById(userId);
        if (!user.getNickname().equals(nickname) && userRepository.existsByNickname(nickname)) {
            throw new BusinessException(UserErrorCode.USER_NICKNAME_DUPLICATED);
        }
        user.changeNickname(nickname);
        return user;
    }

    /**
     * 비밀번호를 바꾼다.
     *
     * <p>호출 측(auth)은 이 메서드 이후 해당 사용자의 refresh token을 모두 폐기해야 한다(PRD FR-USER-05).
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getById(userId);
        if (!matchesPassword(user, currentPassword)) {
            throw new BusinessException(UserErrorCode.USER_PASSWORD_MISMATCH);
        }
        user.changePassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void changeRole(Long userId, Role role) {
        getById(userId).changeRole(role);
    }

    @Transactional
    public void suspend(Long userId) {
        getById(userId).suspend();
    }

    @Transactional
    public void activate(Long userId) {
        getById(userId).activate();
    }

    private void validateDuplicated(String email, String nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(UserErrorCode.USER_EMAIL_DUPLICATED);
        }
        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(UserErrorCode.USER_NICKNAME_DUPLICATED);
        }
    }
}
