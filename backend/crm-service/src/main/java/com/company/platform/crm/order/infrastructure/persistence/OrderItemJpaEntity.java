package com.company.platform.crm.order.infrastructure.persistence;

import com.company.platform.shared.entity.BaseEntityUUID;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * JPA entity for OrderItem (infrastructure layer). Maps to the order_items table in the database.
 */
@Entity
@Table(name = "order_items")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemJpaEntity extends BaseEntityUUID {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private OrderJpaEntity order;

  @Column(name = "product_name", nullable = false, length = 255)
  private String productName;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalPrice;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
