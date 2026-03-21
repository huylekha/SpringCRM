# Shared Messaging Infrastructure

This package provides reusable messaging infrastructure components for microservices using the Outbox and Inbox patterns.

## Components

### Outbox Pattern (`outbox/`)

The Outbox pattern ensures reliable event publishing by storing domain events in the same database transaction as business data.

**Key Classes:**
- `OutboxMessage` - JPA entity for storing events
- `OutboxService` - Service for storing events in the outbox
- `OutboxPublisherWorker` - Background worker that publishes events to Kafka
- `EventTopicMapper` - Interface for mapping event types to Kafka topics

**Usage:**
```java
@Service
public class OrderService {
    private final OutboxService outboxService;
    
    @Transactional
    public void createOrder(Order order) {
        // Save business data
        orderRepository.save(order);
        
        // Store event in outbox (same transaction)
        OrderCreatedEvent event = OrderCreatedEvent.from(order);
        outboxService.storeEvent("Order", order.getId(), "OrderCreatedEvent", event);
    }
}
```

**Configuration:**
Services must provide an `EventTopicMapper` implementation:
```java
@Component
public class MyEventTopicMapper implements EventTopicMapper {
    @Override
    public Optional<String> getTopicForEventType(String eventType) {
        return switch (eventType) {
            case "OrderCreatedEvent" -> Optional.of("my-service.order.created");
            case "OrderUpdatedEvent" -> Optional.of("my-service.order.updated");
            default -> Optional.empty();
        };
    }
}
```

### Inbox Pattern (`inbox/`)

The Inbox pattern ensures idempotent event processing by tracking processed messages.

**Key Classes:**
- `InboxMessage` - JPA entity for tracking processed messages
- `InboxService` - Service for deduplication logic

**Usage:**
```java
@KafkaListener(topics = "order.created")
public void handleOrderCreated(OrderCreatedEvent event) {
    String messageId = event.getEventId();
    
    // Try to process (returns false if already processed)
    if (inboxService.tryProcessMessage(messageId, "OrderCreatedEvent", "order-service")) {
        // Process the event (only runs once)
        processOrderCreated(event);
    }
}
```

### Idempotency (`../idempotency/`)

Provides request-level idempotency using Redis with database fallback.

**Key Classes:**
- `IdempotencyRecord` - JPA entity for database fallback
- `IdempotencyDatabaseService` - Database operations for idempotency
- `RedisIdempotencyStore` - Redis-based implementation with fallback

## Database Setup

Copy the migration templates from `src/main/resources/db/migration-templates/` into your service's migration folder:

1. `outbox_messages_table.sql`
2. `inbox_messages_table.sql` 
3. `idempotency_records_table.sql`

## Configuration

The shared messaging components are auto-configured when:
1. JPA is on the classpath
2. Your application scans the `com.company.platform.shared` package

For Kafka features, ensure Kafka is on the classpath and provide an `EventTopicMapper` bean.

## Dependencies

Add to your service's `pom.xml`:
```xml
<dependency>
    <groupId>com.company.platform</groupId>
    <artifactId>shared-lib</artifactId>
</dependency>
```

The shared-lib includes optional dependencies for Kafka and Redis that will be activated when present in your service.