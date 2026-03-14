# Antigravity Skills Organization

## Primary Skills (use via role tags or auto-routing)

| Skill | Role Tag | Description |
|---|---|---|
| `java-spring-nextjs-fullstack` | *(no tag / cross-layer)* | Fullstack orchestration for BE+FE cross-layer delivery |
| `backend-spring-enterprise` | `#BE` | Spring Boot service/API/data/integration implementation |
| `backend-spring-error-hardening` | `#BE` (hardening) | Anti-HTTP500, exception handling, JPA safety |
| `backend-spring-architecture-large-scale` | `#BE` (architecture) | DDD, large-scale module structure, scalability |
| `frontend-nextjs-enterprise` | `#FE` | Next.js architecture, reusable UI, TanStack/Zustand |
| `mobile-react-native-enterprise` | `#MB` | React Native enterprise architecture |
| `devops-gitlab-docker-cicd` | `#OPS` | GitLab CI/CD, Docker, deploy scripts |
| `qa-testing-enterprise` | `#QC` or `#Test` | QA strategy, quality gates, release sign-off |
| `fullstack-testing-enterprise` | *(testing requests)* | Testing architecture, pyramid, coverage policy |
| `business-analyst-enterprise` | `#BA` | Requirements, acceptance criteria, process analysis |
| `solution-architect-enterprise` | `#SA` | System architecture, integration topology, trade-offs |
| `product-manager-enterprise` | `#PM` | Product roadmap, prioritization, release planning |
| `observability-architect-enterprise` | *(observability)* | Tracing, metrics, dashboards, alerting |

## Usage via Role Tags
```
#BE → backend-spring-enterprise
#FE → frontend-nextjs-enterprise
#MB → mobile-react-native-enterprise
#QC or #Test → qa-testing-enterprise
#OPS → devops-gitlab-docker-cicd
#BA → business-analyst-enterprise
#SA → solution-architect-enterprise
#PM → product-manager-enterprise
```

## Strict Mode Tags
```
#SL → strict-lite (default): minimal gates
#SF → strict-full: full governance, checkpoints, rollback
```

## Multi-Tag Rule
- If multiple role tags exist, **last role tag wins** as primary context.
- Strict mode resolves BEFORE role routing (`#SF` > `#SL`).

## How Antigravity Auto-Routes (no tag needed)
- Backend APIs/services → `backend-spring-enterprise`
- Backend architecture design → `backend-spring-architecture-large-scale`
- Exception/HTTP500 hardening → `backend-spring-error-hardening`
- Frontend/Next.js work → `frontend-nextjs-enterprise`
- CI/CD/Docker/deploy → `devops-gitlab-docker-cicd`
- Testing strategy/quality → `fullstack-testing-enterprise`
- Observability/tracing → `observability-architect-enterprise`
- Cross-layer (BE+FE) → `java-spring-nextjs-fullstack`
