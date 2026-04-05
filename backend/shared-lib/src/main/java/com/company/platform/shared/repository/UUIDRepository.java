package com.company.platform.shared.repository;

import com.company.platform.shared.entity.BaseEntity;
import java.util.UUID;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Specialized repository interface for UUID-based entities. Provides type-safe repository
 * operations for entities extending BaseEntity&lt;UUID&gt;.
 *
 * @param <T> The entity type that extends BaseEntity&lt;UUID&gt;
 */
@NoRepositoryBean
public interface UUIDRepository<T extends BaseEntity<UUID>> extends FullAuditRepository<T, UUID> {}
