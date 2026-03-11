---
name: backend-spring-architecture-large-scale
description: Designs large-scale Spring Boot backend architecture using DDD, clean layering, event-driven integration, and enterprise reliability standards. Use when planning module structure, service boundaries, API strategy, and scalability for high-traffic systems.
---

# Backend Spring Architecture Large Scale

## Apply When
- Designing or refactoring backend architecture for large codebases.
- Defining domain module boundaries and package structure.
- Planning scalability, event-driven flows, and integration patterns.

## Architecture Checklist
- Use feature-based modules with clear bounded contexts.
- Keep strict controller -> service -> repository boundaries.
- Isolate infrastructure concerns (integration, messaging, caching).
- Keep entities internal; expose DTOs at API boundaries.

## Reliability and Performance Checklist
- Centralize exception handling with stable error codes.
- Enforce transaction boundaries in service layer.
- Prevent N+1 and use projections/pagination.
- Add caching and async patterns for high-latency paths.
- Include observability and test strategy in architecture decisions.

## Scalability Checklist
- Prefer low coupling between modules and services.
- Use domain events for cross-module workflows.
- Keep architecture ready for microservice extraction and horizontal scaling.
