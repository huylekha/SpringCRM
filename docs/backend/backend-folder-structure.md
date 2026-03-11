# Backend Folder Structure (Spring Boot Microservices)

## 1. Backend Workspace Layout

```text
backend/
  pom.xml
  shared-lib/
    pom.xml
    src/main/java/com/company/platform/shared/
      exception/
      security/
      response/
      util/
  auth-service/
    pom.xml
    src/main/java/com/company/platform/auth/
      AuthServiceApplication.java
      config/
      auth/
      user/
      role/
      claim/
      permission/
  crm-service/
    pom.xml
    src/main/java/com/company/platform/crm/
      CrmServiceApplication.java
      config/
      customer/
      lead/
      opportunity/
      activity/
      task/
      note/
  api-gateway/
    pom.xml
    src/main/java/com/company/platform/gateway/
      ApiGatewayApplication.java
      config/
      filter/
      route/
      security/
```

## 2. Service Module Internal Pattern

Each business module in `auth-service` and `crm-service` follows:

```text
<module>/
  controller/
  service/
  domain/
  repository/
  query/
  dto/
  mapper/
```

Example for `crm-service/customer`:

```text
customer/
  controller/
    CustomerController.java
  service/
    CustomerService.java
  domain/
    Customer.java
    CustomerStatus.java
  repository/
    CustomerRepository.java
  query/
    CustomerSearchRepository.java
    CustomerSearchRepositoryImpl.java
  dto/
    request/
    response/
  mapper/
    CustomerMapper.java
```

## 3. Layer Responsibilities

- `controller`
  - Request/response mapping, validation, and HTTP status handling.
- `service`
  - Business orchestration, transaction boundaries, and authorization checks.
- `domain`
  - Core business entities/value objects and domain policies.
- `repository`
  - Write and direct lookup persistence.
- `query`
  - QueryDSL search with projection DTO and pageable support.
- `dto`
  - External API contracts and data transport models.
- `mapper`
  - Conversion logic between domain/entity and DTO models.

## 4. Dependency Rules

- `controller` may depend on `service` and `dto`.
- `service` may depend on `domain`, `repository`, `query`, and shared utilities.
- `repository` and `query` depend on persistence and QueryDSL infrastructure.
- `domain` must not depend on `controller` or transport concerns.
- `mapper` must not execute business logic.

## 5. Naming and Package Conventions

- Root package pattern: `com.company.platform.<service>`.
- Class naming:
  - Controllers end with `Controller`.
  - Services end with `Service`.
  - Search repositories end with `SearchRepository`.
  - DTOs suffix with `Request` and `Response`.
- API versioning convention:
  - Base path `/api/v1/...`.

## 6. Data and Query Guidelines

- UUID as PK for all entities.
- QueryDSL in `query` package for dynamic filters only.
- Pageable endpoint contract uses `page`, `size`, and sort list.
- Never return entity directly from controller; map to response DTO.

## 7. Cross-Service Shared Components

Use `backend/shared-lib` for:

- Unified error model (`ApiError`).
- JWT parsing helper abstractions (without service-specific policy logic).
- Base audit utilities and common constants.
- Correlation ID and request context propagation helpers.

## 8. Build and Release Structure

- Parent `backend/pom.xml` manages versions and plugin configuration.
- Child modules:
  - `shared-lib`
  - `auth-service`
  - `crm-service`
  - `api-gateway`
- Each service must build independently for container packaging.
