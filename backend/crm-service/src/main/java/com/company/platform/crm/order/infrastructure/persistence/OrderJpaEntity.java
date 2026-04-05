package com.company.platform.crm.order.infrastructure.persistence;

import com.company.platform.crm.order.domain.OrderStatus;
import com.company.platform.shared.entity.FullAuditEntityUUID;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

/** JPA entity for Order (infrastructure layer). Maps to the orders table in the database. */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderJpaEntity extends FullAuditEntityUUID {

  @Column(name = "order_number", nullable = false, unique = true, length = 50)
  private String orderNumber;

  @Column(name = "customer_id", nullable = false, columnDefinition = "UUID")
  private UUID customerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  @Builder.Default
  private OrderStatus status = OrderStatus.PENDING;

  @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalAmount;

  @Column(nullable = false, length = 3)
  @Builder.Default
  private String currency = "USD";

  @Column(name = "order_date", nullable = false)
  private Instant orderDate;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @OneToMany(
      mappedBy = "order",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @Builder.Default
  private List<OrderItemJpaEntity> items = new ArrayList<>();

  /** Add an item to this order. */
  public void addItem(OrderItemJpaEntity item) {
    items.add(item);
    item.setOrder(this);
  }

  /** Remove an item from this order. */
  public void removeItem(OrderItemJpaEntity item) {
    items.remove(item);
    item.setOrder(null);
  }
}
