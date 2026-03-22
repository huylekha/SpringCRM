# Spring Boot Kubernetes Configuration Guide

This guide explains how to configure Spring Boot applications for Kubernetes deployment, including environment variable mapping, profile activation, and best practices for production readiness.

## 🎯 Configuration Strategy

### Profile Hierarchy

Spring Boot applications use multiple profiles for different environments:

```yaml
# Profile activation in Kubernetes
SPRING_PROFILES_ACTIVE: "k8s,postgres"
```

**Profile Priority (highest to lowest):**
1. `application-k8s.yml` - Kubernetes-specific overrides
2. `application-postgres.yml` - Database-specific configuration  
3. `application.yml` - Base configuration

### Environment Variable Mapping

All external configuration comes from Kubernetes ConfigMaps and Secrets:

```yaml
# ConfigMap values
DB_HOST: postgres
DB_PORT: 5432
DB_NAME: crm_platform_dev  # Environment-specific

# Secret values  
DB_PASSWORD: encrypted_password
JWT_SECRET: encrypted_jwt_secret
```

## 📋 Configuration Templates

### Auth Service Configuration

**File:** `backend/auth-service/src/main/resources/application-k8s.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:postgres}:${DB_PORT:5432}/${DB_NAME:crm_platform}
    username: ${DB_USER:crm_user}
    password: ${DB_PASSWORD:crm_password}
    
  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}

jwt:
  issuer: ${JWT_ISSUER:crm-platform}
  secret: ${JWT_SECRET:}  # Must be provided
  
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  health:
    probes:
      enabled: true  # Enable Kubernetes probes
```

### CRM Service Configuration

**File:** `backend/crm-service/src/main/resources/application-k8s.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:postgres}:${DB_PORT:5432}/${DB_NAME:crm_platform}
    username: ${DB_USER:crm_user}
    password: ${DB_PASSWORD:crm_password}
    
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:}
    producer:
      acks: all
      retries: 3
    consumer:
      group-id: ${spring.application.name}

auth-service:
  url: ${AUTH_SERVICE_URL:http://auth-service:8081}
```

### API Gateway Configuration

**File:** `backend/api-gateway/src/main/resources/application-k8s.yml`

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: ${AUTH_SERVICE_URL:http://auth-service:8081}
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=3
            
        - id: crm-service
          uri: ${CRM_SERVICE_URL:http://crm-service:8082}
          predicates:
            - Path=/api/v1/customers/**,/api/v1/orders/**
          filters:
            - StripPrefix=2
```

## 🔧 Kubernetes Integration

### ConfigMap Structure

```yaml
# k8s/overlays/dev/configmap-patches.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: auth-service-config
data:
  SPRING_PROFILES_ACTIVE: "k8s,postgres"
  DB_HOST: "postgres"
  DB_PORT: "5432"
  DB_NAME: "crm_platform_dev"
  DB_USER: "crm_user"
  REDIS_HOST: "redis"
  REDIS_PORT: "6379"
  JWT_ISSUER: "crm-platform-dev"
```

### Secret Structure

```yaml
# k8s/overlays/dev/secret-patches.yaml
apiVersion: v1
kind: Secret
metadata:
  name: auth-service-secret
type: Opaque
stringData:
  DB_PASSWORD: "dev_password"
  JWT_SECRET: "dev-jwt-secret-64-chars-minimum"
  SENTRY_DSN: "https://sentry.io/project"
```

### Deployment Environment Injection

```yaml
# k8s/base/auth-service/deployment.yaml
containers:
- name: auth-service
  env:
  - name: SPRING_PROFILES_ACTIVE
    valueFrom:
      configMapKeyRef:
        name: auth-service-config
        key: SPRING_PROFILES_ACTIVE
  - name: DB_HOST
    valueFrom:
      configMapKeyRef:
        name: auth-service-config
        key: DB_HOST
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: auth-service-secret
        key: DB_PASSWORD
```

## 🏥 Health Checks

### Actuator Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true  # Enable Kubernetes probes
  health:
    readiness-state:
      enabled: true
    liveness-state:
      enabled: true
```

### Kubernetes Probe Configuration

```yaml
# Readiness probe - when pod is ready to receive traffic
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

# Liveness probe - when pod should be restarted
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 60
  periodSeconds: 30
  timeoutSeconds: 5
  failureThreshold: 3
```

## 📊 Observability

### Metrics Configuration

```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${SPRING_PROFILES_ACTIVE:k8s}
      pod: ${HOSTNAME:unknown}
    distribution:
      percentiles-histogram:
        "http.server.requests": true
  prometheus:
    metrics:
      export:
        enabled: true
```

### Logging Configuration

```yaml
logging:
  level:
    com.company.platform: INFO
    org.springframework.security: WARN
    org.hibernate.SQL: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId:-}] %logger{36} - %msg%n"
```

### Distributed Tracing

```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of requests in production
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://zipkin:9411/api/v2/spans}
```

## 🔒 Security Configuration

### Database Security

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      idle-timeout: 300000
      connection-timeout: 20000
      leak-detection-threshold: 60000
```

### JWT Configuration

```yaml
jwt:
  issuer: ${JWT_ISSUER:crm-platform}
  secret: ${JWT_SECRET:}  # Must be at least 64 characters
  access-token-expiry: 900      # 15 minutes
  refresh-token-expiry: 604800  # 7 days
```

### CORS Configuration

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"  # Restrict in production
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
```

## 🚀 Performance Tuning

### JVM Configuration

```yaml
# In Kubernetes deployment
containers:
- name: auth-service
  env:
  - name: JAVA_OPTS
    value: >-
      -Xms512m
      -Xmx1g
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
      -XX:+UseStringDeduplication
      -Djava.security.egd=file:/dev/./urandom
```

### Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      idle-timeout: 300000
      connection-timeout: 20000
      
  data:
    redis:
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

### Graceful Shutdown

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 20s
```

## 🌍 Environment-Specific Overrides

### Development Environment

```yaml
# k8s/overlays/dev/configmap-patches.yaml
data:
  DB_NAME: "crm_platform_dev"
  JWT_ISSUER: "crm-platform-dev"
  SPRING_PROFILES_ACTIVE: "k8s,postgres,debug"
```

### Staging Environment

```yaml
# k8s/overlays/staging/configmap-patches.yaml
data:
  DB_NAME: "crm_platform_staging"
  JWT_ISSUER: "crm-platform-staging"
  SPRING_PROFILES_ACTIVE: "k8s,postgres"
```

### Production Environment

```yaml
# k8s/overlays/prod/configmap-patches.yaml
data:
  DB_NAME: "crm_platform_prod"
  JWT_ISSUER: "crm-platform-prod"
  SPRING_PROFILES_ACTIVE: "k8s,postgres,prod"
```

## 🔧 Configuration Validation

### Startup Validation

```java
@Component
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {
    
    @NotBlank(message = "JWT secret must not be blank")
    @Size(min = 64, message = "JWT secret must be at least 64 characters")
    private String secret;
    
    @NotBlank(message = "JWT issuer must not be blank")
    private String issuer;
    
    @Min(value = 300, message = "Access token expiry must be at least 5 minutes")
    private int accessTokenExpiry = 900;
    
    // getters and setters
}
```

### Health Check Validation

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("validationQuery", "SELECT 1")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down(e)
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down()
            .withDetail("database", "PostgreSQL")
            .withDetail("error", "Connection validation failed")
            .build();
    }
}
```

## 🐛 Troubleshooting

### Common Configuration Issues

#### 1. Profile Not Activated

**Problem:** Configuration not loading

**Solution:** Check profile activation:
```bash
kubectl logs pod-name | grep "The following profiles are active"
```

#### 2. Environment Variables Not Set

**Problem:** Default values being used

**Solution:** Verify ConfigMap/Secret mounting:
```bash
kubectl describe pod pod-name
kubectl get configmap config-name -o yaml
```

#### 3. Database Connection Issues

**Problem:** Connection timeouts

**Solution:** Check network policies and service discovery:
```bash
kubectl exec -it pod-name -- nslookup postgres
kubectl exec -it pod-name -- telnet postgres 5432
```

#### 4. Health Check Failures

**Problem:** Readiness/liveness probes failing

**Solution:** Test health endpoints:
```bash
kubectl exec -it pod-name -- curl http://localhost:8081/actuator/health
```

### Debug Commands

```bash
# Check environment variables in pod
kubectl exec -it pod-name -- env | grep -E "(DB_|REDIS_|JWT_)"

# View application logs
kubectl logs pod-name --tail=100 -f

# Check configuration
kubectl exec -it pod-name -- curl http://localhost:8081/actuator/configprops

# Test database connectivity
kubectl exec -it postgres-pod -- psql -U crm_user -d crm_platform_dev -c "SELECT 1"
```

## 📋 Configuration Checklist

### Before Deployment

- [ ] All required environment variables defined in ConfigMap
- [ ] All secrets properly encrypted and mounted
- [ ] Profile activation configured correctly
- [ ] Health check endpoints enabled
- [ ] Database connection parameters validated
- [ ] Redis connection parameters validated
- [ ] JWT secret meets minimum length requirement
- [ ] Logging configuration appropriate for environment

### After Deployment

- [ ] Pods start successfully
- [ ] Health checks pass
- [ ] Database connections established
- [ ] Redis connections established
- [ ] Metrics endpoints accessible
- [ ] Application logs show correct profile activation
- [ ] Inter-service communication working

## 🔗 Related Documentation

- [Kubernetes Manifests](../../k8s/)
- [Docker Build Templates](./docker-build-templates.md)
- [GitOps Setup Guide](./k8s-gitops-setup.md)
- [Observability Configuration](../spring-nextjs-observability-tracing-system.mdc)