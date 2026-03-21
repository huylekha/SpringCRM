# IDE Run Configurations

## Prerequisites
1. Ensure Docker infrastructure is running:
   ```bash
   docker compose up -d postgres redis
   ```

2. Verify infrastructure health:
   ```bash
   docker compose ps
   ```

## IntelliJ IDEA / VS Code Run Configurations

### 1. Auth Service (Port 8081)

**Main Class:** `com.company.platform.auth.AuthServiceApplication`

**VM Options:**
```
-Dspring.profiles.active=local,postgres
-Dserver.port=8081
```

**Environment Variables:**
```
SPRING_PROFILES_ACTIVE=local,postgres
SERVER_PORT=8081
```

**Working Directory:** `d:\Azure\Repo\SpringCRM\backend\auth-service`

### 2. CRM Service (Port 8082)

**Main Class:** `com.company.platform.crm.CrmServiceApplication`

**VM Options:**
```
-Dspring.profiles.active=local,postgres
-Dserver.port=8082
```

**Environment Variables:**
```
SPRING_PROFILES_ACTIVE=local,postgres
SERVER_PORT=8082
AUTH_SERVICE_URL=http://localhost:8081
```

**Working Directory:** `d:\Azure\Repo\SpringCRM\backend\crm-service`

### 3. API Gateway (Port 8080)

**Main Class:** `com.company.platform.gateway.ApiGatewayApplication`

**VM Options:**
```
-Dspring.profiles.active=local
-Dserver.port=8080
```

**Environment Variables:**
```
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080
AUTH_SERVICE_URL=http://localhost:8081
CRM_SERVICE_URL=http://localhost:8082
```

**Working Directory:** `d:\Azure\Repo\SpringCRM\backend\api-gateway`

## Testing Endpoints

After starting services, test these endpoints:

```bash
# Health checks
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # CRM Service  
curl http://localhost:8080/actuator/health  # API Gateway

# Database connection test
curl http://localhost:8081/actuator/health/db

# API Gateway routes
curl http://localhost:8080/api/v1/auth/health
```

## Debugging Tips

1. **Enable Debug Logging:**
   - Set `logging.level.com.company.platform=DEBUG` in application-local.yml

2. **Database Issues:**
   ```bash
   # Connect to database
   docker compose exec postgres psql -U crm_user -d crm_platform
   
   # List tables
   \dt
   
   # Check auth_user table
   SELECT * FROM auth_user LIMIT 5;
   ```

3. **Redis Issues:**
   ```bash
   # Test Redis connection
   docker compose exec redis redis-cli ping
   
   # Monitor Redis commands
   docker compose exec redis redis-cli monitor
   ```

4. **Port Conflicts:**
   ```bash
   # Check what's using ports
   netstat -an | findstr :8081
   netstat -an | findstr :8082
   netstat -an | findstr :8080
   ```

## Service Startup Order

1. **Infrastructure:** postgres, redis (via Docker)
2. **Auth Service:** Start first (port 8081)
3. **CRM Service:** Start after auth-service (port 8082)
4. **API Gateway:** Start last (port 8080)

## Development Workflow

1. **Make code changes**
2. **Restart service from IDE** (Ctrl+F5 or equivalent)
3. **Test endpoints**
4. **Check logs in IDE console**

## Frontend Development (Optional)

For frontend development, you can also run it locally:

```bash
cd frontend
npm install
npm run dev
# Frontend will be available at http://localhost:3000
```

Set environment variables for frontend:
```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=dev-nextauth-secret-for-local-development
```