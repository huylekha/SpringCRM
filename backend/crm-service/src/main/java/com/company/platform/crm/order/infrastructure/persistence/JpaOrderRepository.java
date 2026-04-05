package com.company.platform.crm.order.infrastructure.persistence;

import com.company.platform.crm.order.domain.Order;
import com.company.platform.crm.order.domain.OrderRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaOrderRepository implements OrderRepository {

  private final OrderJpaRepository jpaRepository;
  private final OrderMapper orderMapper;

  @Override
  public Order save(Order order) {
    log.debug("Saving order: orderId={}, orderNumber={}", order.getId(), order.getOrderNumber());
    OrderJpaEntity entity = orderMapper.toJpaEntity(order);
    OrderJpaEntity savedEntity = jpaRepository.save(entity);
    return orderMapper.toDomainEntity(savedEntity);
  }

  @Override
  public Optional<Order> findById(String id) {
    log.debug("Finding order by ID: {}", id);
    return jpaRepository
        .findByIdAndDeletedFalse(UUID.fromString(id))
        .map(orderMapper::toDomainEntity);
  }

  @Override
  public Optional<Order> findByOrderNumber(String orderNumber) {
    log.debug("Finding order by order number: {}", orderNumber);
    return jpaRepository
        .findByOrderNumberAndDeletedFalse(orderNumber)
        .map(orderMapper::toDomainEntity);
  }

  @Override
  public boolean existsById(String id) {
    return jpaRepository.existsByIdAndDeletedFalse(UUID.fromString(id));
  }

  @Override
  public boolean existsByOrderNumber(String orderNumber) {
    return jpaRepository.existsByOrderNumberAndDeletedFalse(orderNumber);
  }
}
