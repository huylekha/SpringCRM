package com.company.platform.crm.order.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for OrderJpaEntity. */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, String> {

  /** Find order by ID, excluding soft-deleted records. */
  @Query(
      "SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.items WHERE o.id = :id AND o.deleted = false")
  Optional<OrderJpaEntity> findByIdAndDeletedFalse(String id);

  /** Find order by order number, excluding soft-deleted records. */
  @Query(
      "SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber AND o.deleted = false")
  Optional<OrderJpaEntity> findByOrderNumberAndDeletedFalse(String orderNumber);

  /** Check if order exists by ID, excluding soft-deleted records. */
  boolean existsByIdAndDeletedFalse(String id);

  /** Check if order number exists, excluding soft-deleted records. */
  boolean existsByOrderNumberAndDeletedFalse(String orderNumber);
}
