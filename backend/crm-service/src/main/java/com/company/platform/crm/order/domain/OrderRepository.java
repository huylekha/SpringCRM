package com.company.platform.crm.order.domain;

import java.util.Optional;

/**
 * Domain repository interface for Order (persistence-ignorant). Implementation will be provided by
 * the infrastructure layer.
 */
public interface OrderRepository {

  /** Save an order (create or update). */
  Order save(Order order);

  /** Find an order by ID. */
  Optional<Order> findById(String id);

  /** Find an order by order number. */
  Optional<Order> findByOrderNumber(String orderNumber);

  /** Check if an order exists by ID. */
  boolean existsById(String id);

  /** Check if an order number is already taken. */
  boolean existsByOrderNumber(String orderNumber);
}
