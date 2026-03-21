package com.company.platform.auth.user.domain;

import java.util.Optional;

/**
 * Domain repository interface for User (persistence-ignorant). Implementation will be provided by
 * the infrastructure layer.
 */
public interface UserRepository {

  /** Save a user (create or update). */
  User save(User user);

  /** Find a user by ID. */
  Optional<User> findById(String id);

  /** Find a user by username. */
  Optional<User> findByUsername(String username);

  /** Find a user by email. */
  Optional<User> findByEmail(String email);

  /** Check if a user exists by ID. */
  boolean existsById(String id);

  /** Check if a username is already taken. */
  boolean existsByUsername(String username);

  /** Check if an email is already taken. */
  boolean existsByEmail(String email);
}
