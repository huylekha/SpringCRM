package com.company.platform.shared.repository;

import com.company.platform.shared.entity.BaseEntityLong;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Specialized repository interface for Long-based entities. Provides type-safe repository
 * operations for entities extending BaseEntityLong.
 *
 * @param <T> The entity type that extends BaseEntityLong
 */
@NoRepositoryBean
public interface LongRepository<T extends BaseEntityLong> extends FullAuditRepository<T, Long> {

  // This interface inherits all methods from FullAuditRepository
  // with Long as the ID type, providing type safety for Long-based entities

  // Additional Long-specific methods can be added here if needed
}
