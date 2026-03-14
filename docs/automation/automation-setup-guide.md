# Full Automation Implementation - Setup Guide

## Overview

This guide implements comprehensive automation for the SpringCRM platform, covering:
- ✅ Pre-commit hooks (Husky + lint-staged)
- ✅ Enhanced CI/CD pipeline with quality gates
- ✅ SonarQube static analysis (Docker setup)
- ✅ Security scanning (OWASP + npm audit + Trivy)
- ✅ Spec validation automation
- ✅ Performance testing foundation (Gatling + Lighthouse CI)

**Result**: 100% automation coverage for code quality, security, and performance.

---

## Phase 1: Pre-commit Hooks ✅

### Backend Pre-commit (Spotless + Maven)

**Location**: `.husky/pre-commit`

**What it does**:
- Checks code formatting with Spotless
- Auto-formats if issues detected
- Runs Maven validation

**To test**:
```bash
cd backend
# Make a change
git add .
git commit -m "test: pre-commit hook [CRM-000]"
# Should run Spotless check automatically
```

**To run manually**:
```bash
cd backend
mvn spotless:check  # Check only
mvn spotless:apply  # Auto-format
```

### Frontend Pre-commit (ESLint + Prettier)

**Location**: `frontend/.husky/pre-commit`

**What it does**:
- Runs ESLint on staged TypeScript files
- Formats with Prettier
- Only processes staged files (fast)

**To test**:
```bash
cd frontend
# Make a change
git add .
git commit -m "feat: new component [FRONTEND-001]"
# Should run lint-staged automatically
```

**To run manually**:
```bash
cd frontend
npm run lint:fix
npm run format
```

### Commit Message Validation

**Location**: `.husky/commit-msg`

**Format required**:
```
feat(module): description [FEATURE-ID]

Examples:
- feat(crm): implement customer import [CRM-015]
- fix(auth): resolve token expiry [AUTH-101]
- refactor(frontend): optimize dashboard [FRONTEND-042]
```

**Feature ID prefixes**:
- `CRM-XXX` for CRM features
- `AUTH-XXX` for auth features
- `GATEWAY-XXX` for API gateway
- `FRONTEND-XXX` for frontend features

---

## Phase 2: Quality Gates ✅

### Backend Coverage (JaCoCo)

**Configuration**: `backend/pom.xml`

**Thresholds**:
- Line coverage: 80%
- Branch coverage: 75%

**Run locally**:
```bash
cd backend
mvn clean test jacoco:report
# View report: backend/target/site/jacoco/index.html
```

**CI Job**: `quality_gate_backend_coverage`
- Stage: `quality_gate`
- Fails build if thresholds not met
- Uploads coverage report to GitLab

### Frontend Coverage (Vitest)

**Configuration**: `frontend/vitest.config.ts`

**Thresholds**:
- Lines: 75%
- Branches: 70%
- Functions: 75%
- Statements: 75%

**Run locally**:
```bash
cd frontend
npm run test:coverage
# View report: frontend/coverage/index.html
```

**CI Job**: `quality_gate_frontend_coverage`
- Stage: `quality_gate`
- Fails build if thresholds not met
- Uploads coverage report to GitLab

### Code Formatting Gate

**CI Job**: `quality_gate_spotless`
- Checks backend code formatting
- Fails if Spotless check fails
- Run `mvn spotless:apply` locally to fix

---

## Phase 3: SonarQube Setup ✅

### Local Setup (Docker)

**Start SonarQube**:
```bash
cd devops/sonarqube
bash setup.sh
```

**Default credentials**:
- URL: http://localhost:9000
- Username: `admin`
- Password: `admin` (change on first login)

**Setup steps**:
1. Login and change password
2. Create projects:
   - `springcrm-backend`
   - `springcrm-frontend`
3. Generate token: My Account > Security > Generate Tokens
4. Add to GitLab CI variables:
   - `SONAR_TOKEN`: your-generated-token
   - `SONAR_HOST_URL`: http://localhost:9000

### Backend SonarQube Analysis

**Configuration**: `backend/sonar-project.properties`

**Run locally**:
```bash
cd backend
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=springcrm-backend \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=YOUR_TOKEN
```

**CI Job**: `sonarqube_backend`
- Stage: `quality_gate`
- Runs on MR, develop, main
- Waits for Quality Gate result
- Fails if Quality Gate fails

### Frontend SonarQube Analysis

**Configuration**: `frontend/sonar-project.properties`

**Run locally**:
```bash
cd frontend
npm ci
npm run test:ci
sonar-scanner \
  -Dsonar.projectKey=springcrm-frontend \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=YOUR_TOKEN
```

**CI Job**: `sonarqube_frontend`
- Stage: `quality_gate`
- Runs on MR, develop, main
- Installs sonarqube-scanner
- Waits for Quality Gate result

---

## Phase 4: Security Scanning ✅

### Backend: OWASP Dependency Check

**Configuration**: `backend/pom.xml`

**What it checks**:
- Known CVEs in Maven dependencies
- Fails build if CVSS score ≥ 7 (High/Critical)

**Run locally**:
```bash
cd backend
mvn dependency-check:check
# View report: backend/target/dependency-check-report.html
```

**Suppressing false positives**:
Edit `backend/owasp-suppressions.xml`:
```xml
<suppress>
  <notes><![CDATA[False positive explanation]]></notes>
  <cve>CVE-2024-XXXXX</cve>
</suppress>
```

**CI Job**: `security_backend_dependencies`
- Stage: `security`
- Runs on MR, develop, main
- Uploads HTML/JSON reports

### Frontend: npm audit

**Run locally**:
```bash
cd frontend
npm audit
npm audit fix  # Auto-fix if possible
```

**CI Job**: `security_frontend_dependencies`
- Stage: `security`
- Checks for high/critical vulnerabilities
- Fails build if found
- Runs on MR, develop, main

### Docker Images: Trivy

**What it scans**:
- Docker images for vulnerabilities
- OS packages, application dependencies

**Run locally**:
```bash
trivy image springcrm-auth-service:latest
```

**CI Job**: `security_docker_scan`
- Stage: `security`
- Scans all service images
- Checks for HIGH/CRITICAL vulnerabilities
- Runs after docker_build
- Allow failure: true (warning only for now)

---

## Phase 5: Spec Validation ✅

### Validate Feature Spec

**Script**: `scripts/validate-spec.sh`

**What it checks**:
- All 17 sections present
- No TBD placeholders
- JSON examples in API Contract
- Architecture Decision references
- Audit fields in Domain Model

**Run locally**:
```bash
bash scripts/validate-spec.sh docs/features/crm-015-customer-import.md
```

**Example output**:
```
🔍 Validating Feature Specification: docs/features/crm-015-customer-import.md

📊 Section Completeness: 17/17
✅ Feature Specification validation passed!
```

**CI Job**: `validate_specs`
- Stage: `lint`
- Runs when `docs/features/**/*.md` changes
- Validates all feature specs

### Verify AI Output

**Script**: `scripts/verify-ai-output.sh`

**What it checks**:
- Compares implemented files against Section 17 checklist
- Reports missing files
- Calculates coverage percentage

**Run locally**:
```bash
bash scripts/verify-ai-output.sh \
  docs/features/crm-015-customer-import.md \
  backend/crm-service
```

**Example output**:
```
🔍 Verifying AI-generated output against spec...
   Spec: docs/features/crm-015-customer-import.md
   Implementation: backend/crm-service

Expected files from spec:

  ✅ Found: CustomerImportController.java
  ✅ Found: CustomerImportService.java
  ✅ Found: ImportJobRepository.java
  ❌ Missing: CustomerImportMapper.java

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 Verification Results:
   Total expected: 12 files
   Found: 11 files
   Missing: 1 files
   Coverage: 91%
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Phase 6: Performance Testing ✅

### Backend: Gatling Load Testing

**Configuration**: `backend/pom.xml` + Gatling plugin

**Simulation**: `backend/src/test/java/.../BasicLoadSimulation.java`

**Run locally**:
```bash
cd backend
mvn gatling:test

# With custom parameters:
mvn gatling:test \
  -DBASE_URL=http://localhost:8080 \
  -DUSERS=50 \
  -DRAMP_DURATION=120
```

**Test scenarios**:
- Health check endpoint
- Customer list endpoint (paginated)
- Customer details endpoint

**Performance assertions**:
- Max response time: < 2000ms
- Success rate: > 95%
- Failure rate: < 5%

**Reports**: `backend/target/gatling/`

**CI Job**: `performance_backend_load`
- Stage: `performance`
- Manual trigger only
- Runs on develop, main
- Uploads Gatling HTML report

### Frontend: Lighthouse CI

**Configuration**: `frontend/.lighthouserc.json`

**Run locally**:
```bash
cd frontend
npm run build
npm run start &
npm run lighthouse
```

**Audited URLs**:
- `/` (Homepage)
- `/login`
- `/dashboard`

**Metrics checked**:
- Performance: ≥ 85%
- Accessibility: ≥ 90%
- Best Practices: ≥ 85%
- SEO: ≥ 80%
- First Contentful Paint: < 2000ms
- Largest Contentful Paint: < 2500ms
- Cumulative Layout Shift: < 0.1
- Total Blocking Time: < 300ms

**Reports**: `frontend/.lighthouseci/`

**CI Job**: `performance_frontend_lighthouse`
- Stage: `performance`
- Manual trigger only
- Runs on develop, main
- Uploads Lighthouse reports

---

## GitLab CI Pipeline Stages

New pipeline flow:

```
1. lint
   ├── validate_specs (if specs changed)
   ├── lint_backend
   └── lint_frontend

2. test
   ├── test_backend_unit
   ├── test_backend_integration
   └── test_frontend_unit

3. quality_gate
   ├── quality_gate_backend_coverage
   ├── quality_gate_frontend_coverage
   ├── quality_gate_spotless
   ├── sonarqube_backend
   └── sonarqube_frontend

4. security
   ├── security_backend_dependencies
   ├── security_frontend_dependencies
   └── security_docker_scan

5. build
   ├── build_backend
   └── build_frontend

6. docker_build
   └── docker_build_and_push

7. performance (manual)
   ├── performance_backend_load
   └── performance_frontend_lighthouse

8. deploy
   ├── deploy_staging_*
   └── deploy_production_*
```

---

## Required GitLab CI Variables

Add these to your GitLab project:

**SonarQube** (Settings > CI/CD > Variables):
```
SONAR_TOKEN: <generated-token>
SONAR_HOST_URL: http://localhost:9000
```

**Docker Registry** (if not using GitLab Registry):
```
CI_REGISTRY: your-registry.com
CI_REGISTRY_USER: your-username
CI_REGISTRY_PASSWORD: your-password
```

---

## Troubleshooting

### Pre-commit hooks not running

**Fix**:
```bash
cd frontend
npm run prepare
chmod +x ../.husky/pre-commit
chmod +x .husky/pre-commit
```

### Coverage threshold failures

**Backend**:
```bash
cd backend
mvn clean test jacoco:report
# Review coverage/index.html
# Add tests to increase coverage
```

**Frontend**:
```bash
cd frontend
npm run test:coverage
# Review coverage/index.html
# Add tests to increase coverage
```

### SonarQube connection failed

**Check**:
```bash
# Is SonarQube running?
curl http://localhost:9000

# Is token correct?
echo $SONAR_TOKEN

# Are projects created in SonarQube UI?
```

### OWASP false positives

**Suppress in** `backend/owasp-suppressions.xml`:
```xml
<suppress>
  <notes><![CDATA[
  Explanation of why this is safe
  ]]></notes>
  <gav regex="true">^org\.example:.*:.*$</gav>
  <cve>CVE-2024-XXXXX</cve>
</suppress>
```

### Gatling test failures

**Debug**:
```bash
cd backend
mvn gatling:test -X  # Verbose output

# Check target service is running
curl http://localhost:8080/actuator/health
```

---

## Next Steps

1. **Install dependencies**:
   ```bash
   cd frontend && npm install
   cd ../backend && mvn clean install
   ```

2. **Setup SonarQube**:
   ```bash
   cd devops/sonarqube
   bash setup.sh
   # Follow setup instructions
   ```

3. **Configure GitLab CI variables**:
   - Add SONAR_TOKEN
   - Add SONAR_HOST_URL

4. **Test pre-commit hooks**:
   ```bash
   # Make a small change
   git add .
   git commit -m "test: verify automation [CRM-000]"
   ```

5. **Push to trigger CI pipeline**:
   ```bash
   git push origin feature/automation-setup
   ```

6. **Monitor pipeline**:
   - Go to GitLab CI/CD > Pipelines
   - Watch all stages pass
   - Review quality gate results
   - Check SonarQube reports

---

## Performance Impact

**Pre-commit hooks**:
- Backend: ~5-10 seconds (Spotless check + Maven validate)
- Frontend: ~2-5 seconds (ESLint + Prettier on staged files only)

**CI pipeline additions**:
- Quality gates: +3-5 minutes
- Security scanning: +2-3 minutes
- SonarQube analysis: +2-4 minutes
- Performance tests (manual): +5-10 minutes

**Total CI time**: ~15-20 minutes (without manual performance tests)

---

## Success Metrics

✅ **100% automation coverage**:
- Pre-commit: Code formatting, lint, commit message
- CI: Coverage, security, quality gates, spec validation
- Performance: Load testing, Lighthouse audits

✅ **Zero manual quality checks**:
- All quality gates automated in CI
- Fail-fast on quality/security issues
- Clear actionable feedback

✅ **Faster feedback loop**:
- Pre-commit catches issues in < 10 seconds
- CI fails early (lint → test → quality → security)
- Developers know exactly what to fix

✅ **Production confidence**:
- 80%+ backend coverage, 75%+ frontend coverage
- No high/critical security vulnerabilities
- Performance baselines established
- All specs validated before implementation

---

## Maintenance

**Weekly**:
- Review SonarQube Quality Gate rules
- Check for new security vulnerabilities
- Update suppression files if needed

**Monthly**:
- Update Gatling load test scenarios
- Review Lighthouse performance trends
- Tune coverage thresholds if needed

**Quarterly**:
- Review and update automation rules
- Audit skipped/allowed failures
- Update documentation

---

## Documentation References

- BA Feature Specification Template: `.cursor/rules/ba-feature-specification-template.mdc`
- Backend Testing Rules: `.cursor/rules/spring-testing-ci.mdc`
- Observability Standards: `.cursor/rules/spring-nextjs-observability-tracing-system.mdc`
- Architecture Decisions: `docs/summary.md` (AD-001..014)

---

## Questions?

For issues or questions:
1. Check this README first
2. Review error logs in GitLab CI
3. Check SonarQube project dashboard
4. Review coverage reports (jacoco/vitest)
5. Ask tech lead or team lead

**Remember**: All quality gates are there to protect production. Don't skip them. Fix the root cause instead.
