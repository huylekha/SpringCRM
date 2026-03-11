# Backend

Java 21 + Spring Boot 3 microservice modules.

## Modules

- `shared-lib` - Common utilities, error model, security helpers
- `auth-service` - Identity, JWT, RBAC management
- `crm-service` - CRM domain operations
- `api-gateway` - Edge routing, rate limiting, tracing

## Build

```bash
cd backend
mvn clean package
```
