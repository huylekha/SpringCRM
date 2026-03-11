---
name: backend-spring-enterprise
description: Implements enterprise Spring Boot microservices with clean layering, OpenFeign/Kafka integration, MySQL-Oracle profile strategy, and production reliability patterns.
---

# Backend Spring Enterprise

## Apply When
- Building or refactoring backend services in `backend/*`.
- Designing APIs, database layers, or service-to-service communication.
- Implementing OpenFeign, Kafka, or outbox flows.
- For large-scale architecture blueprinting, prefer `backend-spring-architecture-large-scale`.

## Core Rules
- Enforce Controller -> Service -> Repository/QueryRepository boundaries.
- Use DTO contracts and stable error/response models.
- Keep validation and global exception handling explicit.
- Keep transactions in service layer.

## Integration Rules
- OpenFeign for query/immediate-response commands.
- Kafka for domain events and eventual consistency.
- Use outbox for critical events.

## Data Rules
- MySQL default, Oracle optional via profiles.
- Keep migration strategy vendor-aware and backward compatible.
- Add pagination/indexing/caching with measurable performance intent.
