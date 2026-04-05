package com.company.platform.auth.user.infrastructure.persistence;

import com.company.platform.auth.user.domain.AuthUser;
import com.company.platform.auth.user.domain.User;
import com.company.platform.auth.user.domain.UserRepository;
import com.company.platform.auth.user.repository.AuthUserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaUserRepository implements UserRepository {

  private final AuthUserRepository authUserRepository;
  private final UserMapper userMapper;

  @Override
  public User save(User user) {
    log.debug("Saving user: userId={}, username={}", user.getId(), user.getUsername());
    AuthUser entity = userMapper.toJpaEntity(user);
    AuthUser savedEntity = authUserRepository.save(entity);
    return userMapper.toDomainEntity(savedEntity);
  }

  @Override
  public Optional<User> findById(String id) {
    log.debug("Finding user by ID: {}", id);
    return authUserRepository
        .findByIdAndDeletedFalse(UUID.fromString(id))
        .map(userMapper::toDomainEntity);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    log.debug("Finding user by username: {}", username);
    return authUserRepository
        .findByUsernameAndDeletedFalse(username)
        .map(userMapper::toDomainEntity);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    log.debug("Finding user by email: {}", email);
    return authUserRepository.findByEmailAndDeletedFalse(email).map(userMapper::toDomainEntity);
  }

  @Override
  public boolean existsById(String id) {
    return authUserRepository.findByIdAndDeletedFalse(UUID.fromString(id)).isPresent();
  }

  @Override
  public boolean existsByUsername(String username) {
    return authUserRepository.existsByUsernameAndDeletedFalse(username);
  }

  @Override
  public boolean existsByEmail(String email) {
    return authUserRepository.existsByEmailAndDeletedFalse(email);
  }
}
