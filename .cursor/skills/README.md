# Cursor Skills Organization

## Primary Skills
- `java-spring-nextjs-fullstack`: fullstack orchestration skill for cross-layer delivery.
- `backend-spring-enterprise`: backend implementation standards.
- `backend-spring-error-hardening`: anti-HTTP500 backend hardening skill.
- `backend-spring-architecture-large-scale`: backend architecture design for large systems.
- `frontend-nextjs-enterprise`: frontend implementation standards.
- `mobile-react-native-enterprise`: mobile architecture and implementation standards.
- `qa-testing-enterprise`: QA/testing strategy and release quality gates.
- `business-analyst-enterprise`: business analysis and requirement structuring.
- `solution-architect-enterprise`: end-to-end solution architecture decisions.
- `product-manager-enterprise`: product scope, prioritization, and release planning.
- `devops-gitlab-docker-cicd`: CI/CD, Docker, and deployment standards.
- `fullstack-testing-enterprise`: enterprise testing strategy and quality-gate design.

## Usage
- Role tags force specialist context:
  - `#BE`, `#FE`, `#MB`, `#QC`/`#Test`, `#OPS`, `#BA`, `#SA`, `#PM`
  - if multiple tags exist, use the last role tag as primary context
- Use fullstack skill for end-to-end features requiring BE/FE coordination.
- Use backend large-scale architecture skill first for architecture/system-design requests.
- Use backend hardening skill for exception/validation/JPA safety and HTTP 500 prevention.
- Use backend enterprise skill for standard service/API/data/integration implementation.
- Use frontend skill for Next.js architecture, reusable UI patterns, and anti-pattern prevention.
- For frontend audit/go-live readiness checks, run `frontend-react-anti-patterns-scale-fail.mdc`.
- Use mobile skill for React Native-specific design and implementation.
- Use QA skill when prompt has `#QC` or `#Test` (`#QC == #Test` alias).
- Use BA skill for requirement analysis, scope decomposition, and acceptance criteria.
- Use SA skill for architecture decisions and integration topology.
- Use PM skill for prioritization, roadmap slicing, and release planning.
- Use devops skill for `.gitlab-ci.yml`, Dockerfiles, Compose deployment, and CI variable strategy (primary when prompt has `#OPS`).
- Use testing skill for testing pyramid, test layers, tooling, flaky prevention, and CI test gates.
- If multiple role tags are present in one prompt, use the last role tag as primary context.
