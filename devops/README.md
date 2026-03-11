# DevOps

Docker, Compose, and deployment assets.

## Structure

- `docker/` - docker-compose and related configs
- `scripts/` - deployment and utility scripts

## Deployment Scripts

- `scripts/deploy.sh` - basic environment deploy entrypoint (staging and non-HA scenarios).
- `scripts/deploy-blue-green.sh` - zero-downtime blue/green deployment flow.
- `scripts/rollback.sh` - fast traffic rollback to previous color in blue/green model.

## CI/CD Variables for Blue/Green

- `K8S_NAMESPACE`
- `AUTH_SMOKE_TEST_URL`
- `CRM_SMOKE_TEST_URL`
- `GATEWAY_SMOKE_TEST_URL`
- `FRONTEND_SMOKE_TEST_URL`
- `SMOKE_TEST_RETRIES` (optional, default `12`)
- `SMOKE_TEST_DELAY_SECONDS` (optional, default `5`)
