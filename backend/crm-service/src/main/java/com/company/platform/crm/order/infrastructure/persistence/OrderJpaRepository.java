package com.company.platform.crm.order.infrastructure.persistence;

import com.company.platform.shared.repository.UUIDRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderJpaRepository extends UUIDRepository<OrderJpaEntity> {

  @Query(
      "SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.items WHERE o.id = :id AND o.deleted = false")
  Optional<OrderJpaEntity> findByIdAndDeletedFalse(UUID id);

  @Query(
      "SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber AND o.deleted = false")
  Optional<OrderJpaEntity> findByOrderNumberAndDeletedFalse(String orderNumber);

  boolean existsByIdAndDeletedFalse(UUID id);

  boolean existsByOrderNumberAndDeletedFalse(String orderNumber);
}
