# Docker Compose Example (Local Full Stack)

## 1. Purpose

Run the full CRM platform locally with one command:

```bash
docker compose up -d
```

## 2. Reference Compose File

```yaml
version: "3.9"

services:
  postgres:
    image: postgres:16-alpine
    container_name: crm-postgres
    environment:
      POSTGRES_DB: crm_platform
      POSTGRES_USER: crm_user
      POSTGRES_PASSWORD: crm_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U crm_user -d crm_platform"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - crm_network

  redis:
    image: redis:7-alpine
    container_name: crm-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 10
    networks:
      - crm_network

  auth-service:
    build:
      context: ../../backend/auth-service
      dockerfile: Dockerfile
    container_name: auth-service
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      SERVER_PORT: 8081
      SPRING_PROFILES_ACTIVE: docker,postgres
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: crm_platform
      DB_USER: crm_user
      DB_PASSWORD: crm_password
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_ISSUER: crm-platform
      JWT_PRIVATE_KEY: ${JWT_PRIVATE_KEY}
      JWT_PUBLIC_KEY: ${JWT_PUBLIC_KEY}
      SENTRY_DSN: ${SENTRY_DSN}
    ports:
      - "8081:8081"
    networks:
      - crm_network

  crm-service:
    build:
      context: ../../backend/crm-service
      dockerfile: Dockerfile
    container_name: crm-service
    depends_on:
      postgres:
        condition: service_healthy
      auth-service:
        condition: service_started
    environment:
      SERVER_PORT: 8082
      SPRING_PROFILES_ACTIVE: docker,postgres
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: crm_platform
      DB_USER: crm_user
      DB_PASSWORD: crm_password
      AUTH_SERVICE_URL: http://auth-service:8081
      SENTRY_DSN: ${SENTRY_DSN}
    ports:
      - "8082:8082"
    networks:
      - crm_network

  api-gateway:
    build:
      context: ../../backend/api-gateway
      dockerfile: Dockerfile
    container_name: api-gateway
    depends_on:
      auth-service:
        condition: service_started
      crm-service:
        condition: service_started
      redis:
        condition: service_healthy
    environment:
      SERVER_PORT: 8080
      SPRING_PROFILES_ACTIVE: docker
      AUTH_SERVICE_URL: http://auth-service:8081
      CRM_SERVICE_URL: http://crm-service:8082
      REDIS_HOST: redis
      REDIS_PORT: 6379
      SENTRY_DSN: ${SENTRY_DSN}
    ports:
      - "8080:8080"
    networks:
      - crm_network

  frontend:
    build:
      context: ../../frontend
      dockerfile: Dockerfile
    container_name: crm-frontend
    depends_on:
      api-gateway:
        condition: service_started
    environment:
      NODE_ENV: development
      NEXT_PUBLIC_API_BASE_URL: http://localhost:8080/api/v1
      NEXTAUTH_URL: http://localhost:3000
      NEXTAUTH_SECRET: ${NEXTAUTH_SECRET}
      SENTRY_DSN: ${SENTRY_DSN}
    ports:
      - "3000:3000"
    networks:
      - crm_network

networks:
  crm_network:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
```

## 3. Environment Variables

Create `.env` next to `docker-compose.yml`:

```dotenv
JWT_PRIVATE_KEY=replace_me
JWT_PUBLIC_KEY=replace_me
NEXTAUTH_SECRET=replace_me
SENTRY_DSN=replace_me
```

## 4. Local Startup Validation

After startup, verify:

- Frontend: `http://localhost:3000`
- Gateway: `http://localhost:8080`
- Auth: `http://localhost:8081`
- CRM: `http://localhost:8082`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

## 5. Using MySQL Instead of PostgreSQL

To use MySQL instead of PostgreSQL:

1. Update `devops/docker/docker-compose.yml`:
   - Uncomment the `mysql` service section
   - Comment out the `postgres` service section
   - Update service dependencies from `postgres` to `mysql`
   - Change environment variables:
     - `SPRING_PROFILES_ACTIVE: docker,mysql`
     - `DB_HOST: mysql`
     - `DB_PORT: 3306`
   - Uncomment `mysql_data` volume

2. The system will use MySQL migrations from `db/migration` folders instead of PostgreSQL migrations from `db/migration-postgres`.

## 6. Notes

- Build contexts are reference paths and may be adjusted once repository scaffolding is created.
- Keep Compose file in `devops/docker/docker-compose.yml` for long-term convention.
- Use separate override files for integration-test or performance-test profiles.
- PostgreSQL is the default database. MySQL support is maintained for compatibility.
