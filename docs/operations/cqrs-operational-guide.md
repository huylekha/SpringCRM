# CQRS Operational Guide

## Quick Start

### Local Development Setup

1. **Start Infrastructure**:
   ```bash
   # Start PostgreSQL, Redis, and Kafka
   docker-compose up -d postgres redis kafka
   ```

2. **Run Services**:
   ```bash
   # Start CRM service
   cd backend/crm-service
   mvn spring-boot:run
   
   # Start Auth service  
   cd backend/auth-service
   mvn spring-boot:run
   ```

3. **Verify Health**:
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8081/actuator/health
   ```

### Testing the CQRS Flow

1. **Create an Order** (Command):
   ```bash
   curl -X POST http://localhost:8080/api/v1/orders \
     -H "Content-Type: application/json" \
     -d '{
       "customerId": "customer-123",
       "currency": "USD",
       "notes": "Test order",
       "items": [
         {
           "productName": "Product A",
           "quantity": 2,
           "unitPrice": 10.50
         }
       ]
     }'
   ```

2. **Get Order** (Query):
   ```bash
   curl http://localhost:8080/api/v1/orders/{orderId}
   ```

3. **Check Outbox Processing**:
   ```sql
   SELECT * FROM outbox_messages ORDER BY created_at DESC LIMIT 5;
   ```

4. **Verify Kafka Events**:
   ```bash
   kafka-console-consumer.sh --bootstrap-server localhost:9092 \
     --topic crm.order.created --from-beginning
   ```

## Monitoring & Alerting

### Key Metrics Dashboard

Create Grafana dashboard with these panels:

1. **Request Rate**:
   ```promql
   sum(rate(http_server_requests_total{service="crm-service"}[5m])) by (uri)
   ```

2. **Error Rate**:
   ```promql
   sum(rate(http_server_errors_total{service="crm-service"}[5m])) by (status)
   ```

3. **Response Time**:
   ```promql
   histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))
   ```

4. **Outbox Lag**:
   ```promql
   count(outbox_messages{status="PENDING"})
   ```

5. **Idempotency Hit Rate**:
   ```promql
   rate(idempotency_cache_hits_total[5m]) / rate(idempotency_requests_total[5m])
   ```

### Alert Rules

```yaml
groups:
  - name: cqrs_alerts
    rules:
      - alert: HighErrorRate
        expr: sum(rate(http_server_errors_total[5m])) by (service) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "{{ $labels.service }} has error rate above 5%"

      - alert: OutboxProcessingLag
        expr: count(outbox_messages{status="PENDING"}) > 100
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "Outbox processing lag detected"
          description: "{{ $value }} pending outbox messages"

      - alert: IdempotencyStoreDown
        expr: up{job="redis"} == 0
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Redis idempotency store is down"
          description: "Falling back to database for idempotency"
```

## Troubleshooting Guide

### Common Issues

#### 1. Commands Not Processing

**Symptoms**: HTTP 500 errors, no handler found

**Diagnosis**:
```bash
# Check if handler is registered
curl http://localhost:8080/actuator/beans | grep CommandHandler

# Check application logs
tail -f logs/application.log | grep "CommandBus"
```

**Solutions**:
- Verify handler is annotated with `@Component`
- Check `getCommandType()` returns correct class
- Restart application to refresh handler registry

#### 2. Outbox Messages Stuck

**Symptoms**: Events not appearing in Kafka, outbox table growing

**Diagnosis**:
```sql
-- Check message status distribution
SELECT status, COUNT(*) FROM outbox_messages GROUP BY status;

-- Check recent failures
SELECT * FROM outbox_messages 
WHERE status = 'FAILED' 
ORDER BY created_at DESC LIMIT 10;
```

**Solutions**:
```sql
-- Reset failed messages for retry
UPDATE outbox_messages 
SET status = 'PENDING', retry_count = 0 
WHERE status = 'FAILED' AND created_at > NOW() - INTERVAL 1 HOUR;

-- Check Kafka connectivity
kafka-topics.sh --bootstrap-server localhost:9092 --list
```

#### 3. Idempotency Issues

**Symptoms**: Duplicate processing, cache misses

**Diagnosis**:
```bash
# Check Redis connectivity
redis-cli ping

# Check database fallback usage
SELECT COUNT(*) FROM idempotency_records 
WHERE created_at > NOW() - INTERVAL 1 HOUR;
```

**Solutions**:
```bash
# Clear Redis cache if corrupted
redis-cli FLUSHDB

# Clean up expired database records
DELETE FROM idempotency_records WHERE expires_at < NOW();
```

#### 4. High Memory Usage

**Symptoms**: OutOfMemoryError, GC pressure

**Diagnosis**:
```bash
# Check JVM heap usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Analyze heap dump
jcmd <pid> GC.run_finalization
jcmd <pid> VM.classloader_stats
```

**Solutions**:
- Increase heap size: `-Xmx2g`
- Tune GC settings: `-XX:+UseG1GC`
- Check for memory leaks in handlers
- Review batch sizes in outbox processing

### Performance Tuning

#### Database Optimization

```sql
-- Add indexes for common queries
CREATE INDEX idx_outbox_status_created ON outbox_messages(status, created_at);
CREATE INDEX idx_inbox_message_id ON inbox_messages(message_id);
CREATE INDEX idx_idempotency_key ON idempotency_records(idempotency_key);

-- Optimize connection pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
```

#### Redis Optimization

```yaml
spring.data.redis:
  timeout: 2000ms
  lettuce:
    pool:
      max-active: 8
      max-idle: 8
      min-idle: 0
```

#### Kafka Optimization

```yaml
spring.kafka:
  producer:
    batch-size: 16384
    linger-ms: 5
    buffer-memory: 33554432
  consumer:
    fetch-min-size: 1024
    fetch-max-wait: 500ms
```

## Backup & Recovery

### Database Backup

```bash
# Daily backup script
pg_dump --host=localhost --username=crm_user --dbname=crm_platform \
  --format=custom --file=backup_$(date +%Y%m%d).dump

# Restore from backup
pg_restore --host=localhost --username=crm_user --dbname=crm_platform \
  --clean --if-exists backup_20260321.dump
```

### Event Store Backup

```bash
# Backup Kafka topics
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic crm.order.created --from-beginning \
  --property print.timestamp=true > events_backup.json
```

### Redis Backup

```bash
# Create Redis snapshot
redis-cli BGSAVE

# Copy snapshot file
cp /var/lib/redis/dump.rdb backup/redis_$(date +%Y%m%d).rdb
```

## Capacity Planning

### Scaling Guidelines

#### Vertical Scaling
- **CPU**: 2-4 cores per 1000 TPS
- **Memory**: 2-4 GB heap per instance
- **Storage**: 100 IOPS per 1000 TPS

#### Horizontal Scaling
- **Load Balancer**: Round-robin with health checks
- **Database**: Read replicas for queries
- **Kafka**: Increase partitions for parallelism
- **Redis**: Cluster mode for high availability

### Resource Monitoring

```yaml
# Resource limits in Kubernetes
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"
```

## Security Operations

### Access Control

```yaml
# RBAC permissions for operations
permissions:
  - "order:create"     # Create orders
  - "order:read"       # View orders  
  - "order:update"     # Modify orders
  - "system:monitor"   # View metrics
  - "system:admin"     # Administrative access
```

### Audit Logging

```sql
-- Query audit trail
SELECT 
  created_at,
  created_by,
  aggregate_type,
  aggregate_id,
  event_type
FROM outbox_messages 
WHERE created_at > NOW() - INTERVAL 24 HOUR
ORDER BY created_at DESC;
```

### Security Scanning

```bash
# Dependency vulnerability scan
mvn org.owasp:dependency-check-maven:check

# Container security scan
docker scan crm-service:latest

# Code quality scan
mvn sonar:sonar
```

## Disaster Recovery

### RTO/RPO Targets
- **RTO** (Recovery Time Objective): 15 minutes
- **RPO** (Recovery Point Objective): 5 minutes

### Recovery Procedures

1. **Database Failure**:
   ```bash
   # Promote read replica to master
   kubectl patch postgres-cluster --type='merge' -p='{"spec":{"topology":{"mode":"single-primary"}}}'
   
   # Update application configuration
   kubectl set env deployment/crm-service SPRING_DATASOURCE_URL=jdbc:postgresql://new-master:5432/crm_platform
   ```

2. **Kafka Failure**:
   ```bash
   # Restart Kafka cluster
   kubectl rollout restart statefulset/kafka
   
   # Verify topic replication
   kafka-topics.sh --bootstrap-server localhost:9092 --describe
   ```

3. **Application Failure**:
   ```bash
   # Rolling restart
   kubectl rollout restart deployment/crm-service
   
   # Check health
   kubectl get pods -l app=crm-service
   ```

### Testing Recovery

```bash
# Monthly DR drill script
./scripts/dr-test.sh

# Verify data consistency after recovery
./scripts/verify-data-integrity.sh
```

## Maintenance Windows

### Planned Maintenance

1. **Database Updates**:
   - Schedule during low-traffic hours
   - Use blue-green deployment
   - Test migrations on staging first

2. **Application Updates**:
   - Rolling deployment with health checks
   - Canary releases for major changes
   - Rollback plan ready

3. **Infrastructure Updates**:
   - Kubernetes node rotation
   - OS security patches
   - Network maintenance

### Maintenance Checklist

- [ ] Notify stakeholders 24h in advance
- [ ] Backup all critical data
- [ ] Test rollback procedures
- [ ] Monitor error rates during deployment
- [ ] Verify all health checks pass
- [ ] Update documentation if needed

## Contact Information

### On-Call Escalation

1. **Primary**: Platform Team Lead
2. **Secondary**: Senior Backend Engineer  
3. **Escalation**: Engineering Manager

### Communication Channels

- **Slack**: #platform-alerts
- **Email**: platform-team@company.com
- **PagerDuty**: Platform Engineering

### Runbook Updates

This runbook should be updated:
- After each incident resolution
- When new features are deployed
- During quarterly reviews
- When team members change