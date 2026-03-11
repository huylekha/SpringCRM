# Microservice Architecture Diagram

This document provides the visual architecture views used for development alignment.

## 1. System Context Diagram

```mermaid
flowchart LR
  SalesUser[SalesUser]
  AdminUser[AdminUser]
  FrontendApp[FrontendNextjs]
  ApiGateway[SpringCloudGateway]
  AuthService[AuthService]
  CrmService[CrmService]
  MySqlDb[(MySQL)]
  RedisCache[(Redis)]
  SentryObs[SentryTracing]

  SalesUser --> FrontendApp
  AdminUser --> FrontendApp
  FrontendApp --> ApiGateway
  ApiGateway --> AuthService
  ApiGateway --> CrmService
  AuthService --> MySqlDb
  CrmService --> MySqlDb
  AuthService --> RedisCache
  ApiGateway --> RedisCache
  FrontendApp --> SentryObs
  AuthService --> SentryObs
  CrmService --> SentryObs
  ApiGateway --> SentryObs
```

## 2. Container-Level Request Flow

```mermaid
flowchart TB
  subgraph edge [EdgeLayer]
    FrontendClient[NextjsAppRouter]
    GatewayNode[ApiGateway]
  end

  subgraph services [ServiceLayer]
    AuthNode[AuthService]
    CrmNode[CrmService]
  end

  subgraph data [DataLayer]
    MySqlNode[(MySQL)]
    RedisNode[(Redis)]
  end

  FrontendClient -->|"Bearer JWT"| GatewayNode
  GatewayNode -->|"auth routes"| AuthNode
  GatewayNode -->|"crm routes"| CrmNode
  AuthNode --> MySqlNode
  CrmNode --> MySqlNode
  AuthNode --> RedisNode
  GatewayNode -->|"rate-limit counters"| RedisNode
```

## 3. Authentication Sequence

```mermaid
sequenceDiagram
  participant UserClient as UserClient
  participant Frontend as FrontendApp
  participant Gateway as ApiGateway
  participant Auth as AuthService
  participant Redis as Redis
  participant MySql as MySQL

  UserClient->>Frontend: Submit credentials
  Frontend->>Gateway: POST /auth/login
  Gateway->>Auth: Forward login request
  Auth->>MySql: Validate user and role mapping
  Auth->>Redis: Store refresh token metadata
  Auth-->>Gateway: Return access and refresh tokens
  Gateway-->>Frontend: Tokens response
  Frontend-->>UserClient: Login success and session state update
```

## 4. Protected CRM Request with Refresh Flow

```mermaid
sequenceDiagram
  participant Frontend as FrontendApp
  participant Gateway as ApiGateway
  participant Auth as AuthService
  participant Crm as CrmService
  participant Redis as Redis

  Frontend->>Gateway: GET /crm/customers with access token
  Gateway->>Crm: Forward request with identity context
  Crm-->>Gateway: 401 token expired
  Gateway-->>Frontend: 401 Unauthorized
  Frontend->>Gateway: POST /auth/refresh with refresh token
  Gateway->>Auth: Validate and rotate refresh token
  Auth->>Redis: Check token validity and rotation version
  Auth-->>Gateway: New access token
  Gateway-->>Frontend: Refresh success
  Frontend->>Gateway: Retry GET /crm/customers with new token
  Gateway->>Crm: Forward retried request
  Crm-->>Gateway: 200 customer data
  Gateway-->>Frontend: Response success
```

## 5. Observability and Trace Propagation

```mermaid
flowchart LR
  Browser[FrontendRuntime]
  GatewaySvc[ApiGateway]
  AuthSvc[AuthService]
  CrmSvc[CrmService]
  SentryHub[SentryBackend]

  Browser -->|"error events"| SentryHub
  Browser -->|"trace headers"| GatewaySvc
  GatewaySvc -->|"trace headers"| AuthSvc
  GatewaySvc -->|"trace headers"| CrmSvc
  GatewaySvc -->|"gateway errors"| SentryHub
  AuthSvc -->|"service exceptions"| SentryHub
  CrmSvc -->|"service exceptions"| SentryHub
```

## 6. Diagram Usage Rules

- Use these diagrams as canonical references for onboarding and implementation sequencing.
- Update diagrams when changing service boundaries, token flow, or data topology.
- Keep IDs stable to reduce merge conflicts and maintain documentation consistency.
