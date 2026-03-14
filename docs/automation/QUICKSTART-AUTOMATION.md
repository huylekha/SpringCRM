# Quick Start - Full Automation Setup

## 🚀 5 Bước Setup (15 phút) - Cho GitLab.com

### Bước 1: Cài Dependencies (3 phút)
```bash
# Frontend
cd frontend
npm install

# Backend
cd ../backend
mvn clean install -DskipTests
```

### Bước 2: Cài và Setup Ngrok (2 phút)

**Tại sao cần ngrok?**
- Bạn đang dùng **GitLab.com** với shared runners
- Shared runners không thể access `localhost` của máy bạn
- Ngrok expose local SonarQube ra internet để CI có thể kết nối

**Cài ngrok**:
1. Vào https://ngrok.com/download
2. Download Windows version
3. Giải nén và chạy `ngrok.exe`

**Đăng ký ngrok** (Free):
1. Vào https://dashboard.ngrok.com/signup
2. Đăng ký tài khoản miễn phí
3. Vào https://dashboard.ngrok.com/get-started/your-authtoken
4. Copy authtoken

**Authenticate ngrok**:
```bash
# PowerShell hoặc CMD
ngrok config add-authtoken YOUR_AUTHTOKEN
```

### Bước 3: Start SonarQube (3 phút)

**Terminal 1 - Start SonarQube**:
```bash
cd D:\Azure\Repo\SpringCRM\devops\sonarqube

# Option A: Dùng Docker Compose trực tiếp (nếu bash lỗi line endings)
docker-compose up -d

# Option B: Dùng Git Bash
bash setup.sh
```

**Đợi 2-3 phút** cho SonarQube khởi động.

**Check status**:
```bash
docker-compose ps

# Output mong đợi:
# NAME            IMAGE                       STATUS
# sonarqube       sonarqube:10.4-community    Up 2 minutes
# sonarqube-db    postgres:16-alpine          Up 2 minutes
```

### Bước 4: Start Ngrok và Setup SonarQube (5 phút)

**Terminal 2 - Start ngrok**:
```bash
# PowerShell hoặc CMD mới
ngrok http 9000
```

**⚠️ QUAN TRỌNG**: Copy URL từ output ngrok:
```
Forwarding   https://abc123def456.ngrok-free.app -> http://localhost:9000
             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
             Copy URL này!
```

**Setup SonarQube**:
1. **Mở trình duyệt**, paste URL ngrok (ví dụ: `https://abc123def456.ngrok-free.app`)
2. Ngrok warning page → Click **"Visit Site"**
3. SonarQube login page hiện ra:
   - Username: `admin`
   - Password: `admin`
4. Đổi password khi được yêu cầu (ví dụ: `Admin@123`)

**Tạo Projects**:
1. Click **Projects** > **Create Project** > **Manually**
2. **Project 1**:
   - Project key: `springcrm-backend`
   - Display name: `SpringCRM Backend`
   - Click **Set Up**
3. Click **Projects** > **Create Project** lại
4. **Project 2**:
   - Project key: `springcrm-frontend`
   - Display name: `SpringCRM Frontend`
   - Click **Set Up**

**Generate Token**:
1. Click icon user (góc phải trên) → **My Account**
2. Tab **Security**
3. **Generate Tokens**:
   - Name: `GitLab CI`
   - Type: `Global Analysis Token`
   - Expires in: `90 days` (hoặc `No expiration`)
   - Click **Generate**
4. **⚠️ Copy token ngay** (chỉ hiển thị 1 lần!):
   - Ví dụ: `squ_abc123def456ghi789jkl012mno345pqr678`

### Bước 5: Add GitLab CI Variables (2 phút)

**Vào GitLab.com**:
1. Mở repository: `https://gitlab.com/your-username/SpringCRM`
2. **Settings** (sidebar trái) → **CI/CD**
3. **Variables** section → Click **Expand**
4. Click **Add variable**

**Variable 1 - SONAR_TOKEN**:
```
Key: SONAR_TOKEN
Value: squ_abc123def456ghi789jkl012mno345pqr678  (token vừa copy)
Type: Variable
Flags: ✅ Mask variable
       ⬜ Protect variable (KHÔNG check)
       ⬜ Expand variable reference
```
→ Click **Add variable**

**Variable 2 - SONAR_HOST_URL**:
```
Key: SONAR_HOST_URL
Value: https://abc123def456.ngrok-free.app  (URL ngrok vừa copy)
Type: Variable
Flags: ⬜ Mask variable (không cần)
       ⬜ Protect variable
       ⬜ Expand variable reference
```
→ Click **Add variable**

**Verify**:
```
CI/CD Variables:
┌─────────────────────┬──────────────────────────┬──────────┐
│ Key                 │ Value                    │ Options  │
├─────────────────────┼──────────────────────────┼──────────┤
│ SONAR_TOKEN         │ ************************ │ Masked   │
│ SONAR_HOST_URL      │ https://abc123.ngrok...  │          │
└─────────────────────┴──────────────────────────┴──────────┘
```

---

## ✅ Test Local

### Test Pre-commit Hooks
```bash
cd ../..
echo "// test" >> frontend/src/app/page.tsx
git add frontend/src/app/page.tsx
git commit -m "test: pre-commit hook [CRM-000]"
```

Kết quả mong đợi:
```
🔍 Running lint-staged for frontend...
✅ Pre-commit checks passed for frontend!
🔍 Validating commit message...
✅ Commit message references Feature ID
```

### Test Backend Coverage
```bash
cd backend
mvn clean test jacoco:report
```

Mở: `backend/target/site/jacoco/index.html`

### Test Frontend Coverage
```bash
cd frontend
npm run test:coverage
```

Mở: `frontend/coverage/index.html`

---

## 🚀 Push và Monitor Pipeline

```bash
git push origin your-branch
```

Vào GitLab: CI/CD > Pipelines

Pipeline flow:
```
1. ✅ lint (validate_specs, lint_backend, lint_frontend)
2. ✅ test (unit tests, integration tests)
3. ✅ quality_gate (coverage, spotless, sonarqube) ← MỚI
4. ✅ security (OWASP, npm audit, Trivy) ← MỚI
5. ✅ build (backend, frontend)
6. ✅ docker_build (images)
7. ⏭️  performance (manual) ← MỚI
8. ✅ deploy (staging/production)
```

---

## 📊 Xem Kết Quả

### Coverage Reports
- GitLab: CI/CD > Pipelines > [your-pipeline] > Tests tab
- Backend: Tải artifact `backend/target/site/jacoco/`
- Frontend: Tải artifact `frontend/coverage/`

### SonarQube
- Mở http://localhost:9000
- Projects > `springcrm-backend` hoặc `springcrm-frontend`
- Xem: Code Smells, Bugs, Vulnerabilities, Technical Debt

### Security Reports
- GitLab: CI/CD > Pipelines > [your-pipeline] > Security tab
- OWASP: Tải artifact `backend/target/dependency-check-report.html`
- npm audit: Xem logs của job `security_frontend_dependencies`

---

## 🔧 Troubleshooting

### Lỗi: setup.sh line endings (Windows)
```bash
# Lỗi: setup.sh: line 2: $'\r': command not found

# Fix Option 1: Dùng Git Bash
bash setup.sh

# Fix Option 2: Chạy Docker Compose trực tiếp
docker-compose up -d

# Fix Option 3: Dùng WSL
wsl
bash setup.sh
```

### Lỗi: ngrok "command not found"
```bash
# Chưa cài ngrok hoặc chưa thêm vào PATH

# Fix:
# 1. Download từ https://ngrok.com/download
# 2. Giải nén
# 3. Chạy từ thư mục giải nén: .\ngrok.exe http 9000
# Hoặc thêm vào PATH để chạy từ mọi nơi
```

### Lỗi: ngrok "Failed to complete tunnel connection"
```bash
# Chưa authenticate

# Fix:
ngrok config add-authtoken YOUR_AUTHTOKEN

# Get token tại: https://dashboard.ngrok.com/get-started/your-authtoken
```

### Lỗi: CI "SonarQube server cannot be reached"
**Nguyên nhân**: 
- Ngrok terminal đã đóng
- URL ngrok không đúng
- SonarQube chưa khởi động xong

**Fix**:
```bash
# 1. Check ngrok terminal vẫn đang chạy (KHÔNG được đóng)
# Terminal phải hiện:
# Forwarding   https://abc123.ngrok-free.app -> http://localhost:9000

# 2. Check SonarQube đang chạy
docker ps | grep sonarqube
# Phải thấy: sonarqube ... Up X minutes

# 3. Test URL ngrok trong trình duyệt
# Mở: https://abc123.ngrok-free.app
# Click "Visit Site" → phải thấy SonarQube

# 4. Verify GitLab CI variable SONAR_HOST_URL
# GitLab > Settings > CI/CD > Variables
# SONAR_HOST_URL phải đúng URL ngrok hiện tại
```

### Lỗi: "Unauthorized: Invalid token"
```bash
# Token sai hoặc expired

# Fix:
# 1. Login SonarQube qua ngrok URL
# 2. My Account > Security > Generate new token
# 3. Update SONAR_TOKEN trong GitLab CI Variables
```

### Lỗi: Pre-commit quá chậm
```bash
# Chỉ lint staged files (đã config sẵn)
# Nếu vẫn chậm, check số lượng files staged:
git diff --cached --name-only | wc -l
```

### Lỗi: Coverage không đủ threshold
```bash
# Xem file nào thiếu coverage
cd backend && mvn jacoco:report
cd frontend && npm run test:coverage

# Thêm tests cho các file thiếu coverage
```

### Lỗi: OWASP false positive
Edit `backend/owasp-suppressions.xml`:
```xml
<suppress>
  <notes><![CDATA[
  Lý do: False positive, library chỉ dùng internal
  ]]></notes>
  <cve>CVE-2024-XXXXX</cve>
</suppress>
```

---

## ⚠️ Lưu Ý Quan Trọng Khi Dùng Ngrok

### 1. Ngrok Terminal PHẢI Giữ Mở
- **KHÔNG được đóng** terminal đang chạy ngrok
- Đóng terminal = ngrok stop = CI không chạy được
- Nên minimize window thay vì đóng

### 2. URL Ngrok Thay Đổi Khi Restart
Mỗi lần restart ngrok, URL sẽ khác:
```
Lần 1: https://abc123.ngrok-free.app
Lần 2: https://xyz789.ngrok-free.app  ← Khác!
```

**Khi restart ngrok**:
1. Copy URL mới từ ngrok terminal
2. Vào GitLab > Settings > CI/CD > Variables
3. Edit `SONAR_HOST_URL` với URL mới

### 3. Giữ URL Ngrok Cố Định (Optional)
**Free**: Dùng static domain
1. Vào https://dashboard.ngrok.com/domains
2. Claim free static domain (ví dụ: `your-name-123.ngrok-free.app`)
3. Chạy: `ngrok http --domain=your-name-123.ngrok-free.app 9000`

**Paid** (~$8/tháng):
- Custom domain
- Nhiều tunnels đồng thời
- Không có ngrok warning page

### 4. Alternative: Deploy SonarQube Lên Server
Nếu muốn long-term stable:
- Deploy SonarQube lên VPS (DigitalOcean, AWS, Azure...)
- Hoặc dùng SonarCloud (https://sonarcloud.io) - managed service

---

## 📚 Tài Liệu Chi Tiết

- **Setup đầy đủ**: `docs/automation-setup-guide.md` (500+ dòng)
- **Tổng kết**: `docs/automation-implementation-summary.md`
- **Checklist**: `docs/automation-implementation-checklist.md`

---

## ⚡ Các Lệnh Hay Dùng

### Ngrok (GitLab.com users)
```bash
# Start ngrok (Terminal riêng, giữ mở)
ngrok http 9000

# Check ngrok status
curl http://localhost:4040/api/tunnels

# Restart ngrok với static domain (nếu có)
ngrok http --domain=your-name.ngrok-free.app 9000

# Stop ngrok
Ctrl+C trong terminal đang chạy ngrok
```

### Backend
```bash
cd backend
mvn spotless:apply              # Auto-format code
mvn clean test jacoco:report    # Run tests + coverage
mvn dependency-check:check      # Security scan
mvn gatling:test                # Load testing
```

### Frontend
```bash
cd frontend
npm run lint:fix                # Fix linting issues
npm run format                  # Format with Prettier
npm run test:coverage           # Run tests + coverage
npm run lighthouse              # Lighthouse audit
```

### SonarQube
```bash
# Start SonarQube
cd devops/sonarqube
docker-compose up -d

# Stop SonarQube
docker-compose down

# Check logs
docker-compose logs sonarqube

# Restart SonarQube
docker-compose restart

# Reset SonarQube (xóa data)
docker-compose down -v
```

### CI
```bash
# Validate feature spec
bash scripts/validate-spec.sh docs/features/your-spec.md

# Verify AI output
bash scripts/verify-ai-output.sh docs/features/your-spec.md backend/your-service
```

---

## 🎯 Success Metrics (Tuần Đầu)

Sau khi merge và chạy pipeline lần đầu, check:

- [ ] ✅ Ngrok đang chạy và có URL forwarding
- [ ] ✅ SonarQube access được qua ngrok URL
- [ ] ✅ GitLab CI variables đã add (SONAR_TOKEN, SONAR_HOST_URL)
- [ ] ✅ Pipeline pass tất cả stages (lint → test → quality_gate → security)
- [ ] 📊 Backend coverage ≥ 80% line, ≥ 75% branch
- [ ] 📊 Frontend coverage ≥ 75% lines/functions/statements
- [ ] 🔒 Không có high/critical security vulnerabilities
- [ ] 🏆 SonarQube Quality Gate = PASSED
- [ ] ⏱️ Pipeline time < 20 phút (không tính performance tests)

**Quick Check Commands**:
```bash
# 1. Check ngrok running
# Terminal ngrok phải show:
# Forwarding   https://xxx.ngrok-free.app -> http://localhost:9000

# 2. Check SonarQube running
docker ps | grep sonarqube
# Output: sonarqube ... Up X minutes

# 3. Test ngrok URL
curl https://abc123.ngrok-free.app
# Hoặc mở trong browser, click "Visit Site"

# 4. Check GitLab variables
# GitLab > Settings > CI/CD > Variables
# Phải có: SONAR_TOKEN (masked), SONAR_HOST_URL
```

---

## 🔒 Strict Mode Workflow (Advanced)

### Overview

**Strict Mode** thực thi tests và coverage checks **tại local** trước khi commit, đảm bảo code quality cao nhất.

**Khi nào dùng?**
- ✅ Development environment: Máy dev với đủ tài nguyên
- ✅ Critical features: Code liên quan tới security, payment, data integrity
- ✅ Team standards: Yêu cầu zero defect trước khi push

**Trade-off**:
- ⏱️ Commit time: 30 giây - 2 phút (tùy service)
- ✅ Quality: Tests run local, catch bugs sớm nhất
- 💰 CI cost: Giảm CI failures, tiết kiệm CI minutes

### How It Works

**Pre-commit Hook** (`.husky/pre-commit`):
1. **Detect changes**: Phân tích staged files để biết service nào thay đổi
2. **Run tests**: Chỉ test services đã thay đổi (không test all)
3. **Check coverage**: JaCoCo (backend) và Vitest (frontend) enforce thresholds
4. **Abort if fail**: Commit bị block nếu tests fail hoặc coverage thấp

**Selective Testing**:
```
backend/auth-service/**      → Test auth-service only
backend/crm-service/**       → Test crm-service only
backend/api-gateway/**       → Test api-gateway only
backend/shared-lib/**        → Test ALL backend services (vì shared)
frontend/**                  → Test frontend only
```

### Expected Commit Times

| Change Scope | Time | Tests Run |
|-------------|------|-----------|
| Frontend only | 30-60s | Frontend unit tests + coverage |
| Single backend service | 45-90s | Service unit tests + JaCoCo |
| Multiple services | 1-2 min | All changed services |
| Shared-lib change | 2-3 min | ALL backend services (vì shared-lib affects all) |

### Example Workflow

**Scenario 1: Thay đổi auth-service**
```bash
# Edit file
vim backend/auth-service/src/.../UserService.java

# Add tests
vim backend/auth-service/src/test/.../UserServiceTest.java

# Commit (strict hook tự chạy)
git add .
git commit -m "feat(auth): add user profile endpoint [AUTH-023]"

# Output từ pre-commit:
# 🔒 STRICT MODE: Running comprehensive pre-commit checks...
# 
# 📊 Changed services detected:
#   ✓ auth-service
# 
# 📦 Backend changes detected, running strict checks...
# 
# ▶️  Checking code formatting with Spotless...
#    ✓ Formatting passed
# 
# ▶️  Testing auth-service with coverage...
#    ✓ auth-service tests passed with coverage
# 
# ✅ All backend checks passed!
# ✨ All strict pre-commit checks passed!
```

**Scenario 2: Thay đổi shared-lib** (impacts tất cả services)
```bash
# Edit shared error handling
vim backend/shared-lib/src/.../exception/ErrorCode.java

git add .
git commit -m "refactor(shared): add new error codes [CRM-100]"

# Output:
# 📊 Changed services detected:
#   ✓ auth-service
#   ✓ crm-service
#   ✓ api-gateway
#   ✓ shared-lib (affects all backend)
# 
# ▶️  Testing auth-service with coverage...
#    ✓ auth-service tests passed
# ▶️  Testing crm-service with coverage...
#    ✓ crm-service tests passed
# ▶️  Testing api-gateway with coverage...
#    ✓ api-gateway tests passed
# 
# Time: ~2-3 minutes (tests 3 services)
```

### Coverage Thresholds

**Backend (JaCoCo)**:
- Line coverage: **80%** minimum
- Branch coverage: **75%** minimum

**Frontend (Vitest)**:
- Lines: **75%** minimum
- Branches: **70%** minimum
- Functions: **70%** minimum
- Statements: **70%** minimum

**Nếu coverage thấp**:
```bash
# Backend: See detailed coverage report
cd backend
mvn jacoco:report
# Open: backend/<service>/target/site/jacoco/index.html

# Frontend: See detailed coverage report
cd frontend
npm run test:coverage
# Open: frontend/coverage/index.html
```

### Emergency Bypass

**Khi nào dùng?**
- 🚨 Hotfix critical bug trong production
- ⏰ Urgent deployment cần ship ngay
- 🔧 Fix CI config (không cần test)

**Cách dùng**:
```bash
# Skip ALL pre-commit hooks (logged for audit)
SKIP_HOOKS=1 git commit -m "hotfix: fix payment crash [URGENT]"

# Hook sẽ log bypass vào .git/hooks-bypass.log:
# [2026-03-14 10:30:45] User: dev@company.com | Branch: main | SKIP_HOOKS=1
#   Staged files:
#     - backend/payment-service/src/.../PaymentService.java
```

**⚠️ Warning**:
- Bypass được log để audit
- CI vẫn chạy full tests
- Chỉ dùng khi **thực sự cần thiết**

---

## 🤖 AI Code Review

### Overview

**AI Code Review** phân tích tất cả Git changes và đưa ra báo cáo quality toàn diện trước khi commit.

**Kiểm tra gì?**
- ✅ Architecture compliance (AD-001..014)
- ✅ Code quality (null safety, error handling, transactions)
- ✅ Security (SQL injection, XSS, auth checks)
- ✅ Performance (N+1 queries, missing indexes)
- ✅ Test coverage (missing tests for changed logic)

### Trigger Modes

#### Auto-Review (Recommended)

AI tự động review sau khi code xong:
```
[You finish implementing feature]

AI: "I've completed the user registration feature. 
     Would you like me to review the changes? (yes/no/skip)"

[You type: yes]

AI: [Analyzes git diff]
    [Generates detailed report]
    "Review complete! Found 1 warning in UserController.java - 
     missing input validation on email field. Should I fix it?"
```

#### Manual Review

**Full review** (all changes):
```bash
# Trong chat Cursor, gõ:
#Review

# AI sẽ:
# 1. Analyze git diff (staged + unstaged)
# 2. Check architecture compliance
# 3. Find security/performance issues
# 4. Generate report với code examples
```

**Backend-only review**:
```bash
#ReviewBE

# Chỉ review backend changes
# Bỏ qua frontend/docs/config
```

**Frontend-only review**:
```bash
#ReviewFE

# Chỉ review frontend changes
# Bỏ qua backend files
```

### Review Report Structure

```markdown
# Code Review Report

**Quality Score:** 92/100
- Architecture Compliance: 24/25
- Code Quality: 23/25
- Security: 23/25
- Test Coverage: 22/25

## Changed Services & Files
### Backend Services
- auth-service (3 files)
  - UserController.java
  - UserService.java
  - UserServiceTest.java

## 🔴 Critical Issues (Must Fix Before Commit)

#### Issue 1: Missing Input Validation
**File:** `backend/auth-service/.../UserController.java:45`
**Category:** Security

**Problem:**
Email parameter not validated, potential for invalid data.

**Current Code:**
```java
@PostMapping("/register")
public ResponseEntity<UserResponse> register(@RequestBody CreateUserRequest request) {
    return userService.createUser(request);
}
```

**Fix:**
```java
@PostMapping("/register")
public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
    return userService.createUser(request);
}
```

## 🟡 Warnings (Should Fix)
[List of non-critical issues]

## Next Steps
1. Fix critical issue in UserController.java
2. Re-run review: Type #Review
3. Proceed to commit when clean
```

### Acting on Review Findings

**Option 1: Fix Issues Yourself**
```bash
# Read report
# Make changes manually
# Re-run review
#Review

# AI: "All issues resolved! Ready to commit."
```

**Option 2: Ask AI to Fix**
```
You: "Fix issue 1 in UserController.java"

AI: [Reads file]
    [Applies fix]
    "Fixed! Added @Valid annotation and input validation."
    
You: "#Review"  # Verify fix

AI: "Review clean! No critical issues."
```

**Option 3: Proceed Anyway (with bypass)**
```bash
# If you understand the risk
SKIP_HOOKS=1 git commit -m "feat: user registration [AUTH-023]"

# Note: CI will still catch issues
```

### Integration with Strict Mode

**Recommended workflow**:
```
1. Code feature
2. AI auto-review (or manual #Review)
3. Fix critical issues
4. git add .
5. git commit (strict pre-commit runs tests)
6. git push (CI runs full pipeline)
```

**Timeline**:
- AI Review: 30-60 seconds (analysis + report)
- Pre-commit: 30s-2min (tests + coverage)
- CI Pipeline: 5-10 minutes (full validation)

**Total time**: ~10 minutes from code complete to merged
**Quality**: Near-zero defects reach production

### Customizing Auto-Review

**Disable auto-review** (if you prefer manual only):
```bash
# Add to .bashrc hoặc .zshrc:
export AUTO_REVIEW=false

# AI sẽ không tự review, phải dùng #Review
```

**Enable for critical files only**:
- AI tự detect critical changes (security, payments, auth)
- Luôn trigger auto-review cho những files này
- File bình thường: manual review only

---

## 📊 Selective CI Pipeline

### How It Works

**CI chỉ test services có code thay đổi**, tiết kiệm thời gian và CI resources.

**GitLab CI stages**:
```
.pre: detect_changes          # Phân tích git diff
 ↓
lint: lint_backend/frontend   # Always run (fast)
 ↓
test: test_auth/crm/gateway   # Conditional (based on changes)
 ↓
build: build_auth/crm/gateway # Conditional
 ↓
docker_build: docker_*        # Conditional
 ↓
deploy: deploy_staging/prod   # Manual
```

### Change Detection

**detect_changes job** (`.pre` stage):
```bash
# Compares: git diff $CI_COMMIT_BEFORE_SHA..$CI_COMMIT_SHA

# Sets environment variables:
CHANGED_AUTH=true/false
CHANGED_CRM=true/false
CHANGED_GATEWAY=true/false
CHANGED_SHARED=true/false    # If true, all backend affected
CHANGED_FRONTEND=true/false
```

**Downstream jobs check flags**:
```yaml
test_auth_service:
  rules:
    - if: $CHANGED_AUTH == "true"
    - changes:
      - backend/auth-service/**/*
```

### Pipeline Examples

**Example 1: Change auth-service only**
```
✅ detect_changes (sets CHANGED_AUTH=true)
✅ lint_backend (always)
✅ lint_frontend (always)
✅ test_auth_service (conditional: run)
⏭️  test_crm_service (skipped)
⏭️  test_api_gateway (skipped)
⏭️  test_frontend (skipped)
✅ build_auth_service (conditional: run)
⏭️  build_crm/gateway/frontend (skipped)

Total time: ~3 minutes (vs 10+ for full pipeline)
```

**Example 2: Change shared-lib**
```
✅ detect_changes (sets CHANGED_SHARED=true → all backend=true)
✅ lint_backend
✅ lint_frontend
✅ test_auth_service (conditional: run)
✅ test_crm_service (conditional: run)
✅ test_api_gateway (conditional: run)
⏭️  test_frontend (skipped)
✅ build_auth_service
✅ build_crm_service
✅ build_api_gateway
⏭️  build_frontend (skipped)

Total time: ~8 minutes (all backend, skip frontend)
```

**Example 3: Change frontend only**
```
✅ detect_changes (sets CHANGED_FRONTEND=true)
✅ lint_backend
✅ lint_frontend
⏭️  test_auth/crm/gateway (skipped)
✅ test_frontend (conditional: run)
⏭️  build_auth/crm/gateway (skipped)
✅ build_frontend (conditional: run)

Total time: ~2 minutes (frontend only)
```

### Benefits

**Time Savings**:
- Frontend-only changes: **80% faster** (2 min vs 10 min)
- Single backend service: **70% faster** (3 min vs 10 min)
- Multiple services: **30-50% faster** (5-7 min vs 10 min)

**Cost Savings**:
- Fewer CI minutes used
- Less queue time on shared runners
- Faster feedback loop

**Always-Run Jobs** (fast, critical):
- `lint_backend` - Maven validate (10s)
- `lint_frontend` - ESLint (15s)
- `detect_changes` - Git diff analysis (5s)

---

## 📋 Daily Workflow (Updated)

**Trước khi code**:
```bash
# 1. Start SonarQube (nếu chưa chạy)
cd devops/sonarqube
docker-compose up -d

# 2. Start ngrok (Terminal riêng, GIỮ MỞ)
ngrok http 9000

# 3. Copy URL ngrok (nếu restart)
# https://abc123.ngrok-free.app
```

**Khi code**:
```bash
# Code như bình thường
# AI auto-review hoặc manual #Review khi xong
git add .
git commit -m "feat: new feature [CRM-001]"
# Strict pre-commit runs (30s-2min)
git push
# Selective CI runs (2-10min depending on changes)
```

**Workflow hoàn chỉnh**:
```
Code → AI Review → Fix Issues → Pre-commit Tests → CI Pipeline → Deploy
  ↓         ↓           ↓              ↓                ↓           ↓
30min   30-60s      5-10min        30s-2min         2-10min    Manual
```

**CI tự động chạy**:
1. ✅ Detect changes (5s)
2. ✅ Lint + validate specs (20s)
3. ✅ Unit tests (conditional, 1-5min)
4. ✅ Coverage checks (80%+ backend, 75%+ frontend)
5. ✅ SonarQube quality analysis (conditional)
6. ✅ Security scans (OWASP + npm audit, conditional)
7. ✅ Build + Docker images (conditional)
8. ✅ Deploy (manual trigger)

**Xem kết quả**:
- GitLab: CI/CD > Pipelines
- SonarQube: `https://abc123.ngrok-free.app` > Projects
- Coverage: Pipeline artifacts > coverage reports

---

## 🆘 Cần Giúp?

1. **Setup issues**: Đọc `docs/automation-setup-guide.md`
2. **CI failures**: Check GitLab logs của stage đang fail
3. **Coverage thấp**: Xem coverage reports, thêm tests
4. **False positives**: Add suppressions với explanation
5. **Pipeline quá chậm**: Tối ưu stage đang chậm nhất

---

## ✅ Xong!

Bạn đã setup xong enterprise-grade automation cho SpringCRM với GitLab.com + ngrok!

**Automation coverage**: 100%
**Manual quality gates**: 0%

### 📋 Daily Workflow

**Trước khi code**:
```bash
# 1. Start SonarQube (nếu chưa chạy)
cd devops/sonarqube
docker-compose up -d

# 2. Start ngrok (Terminal riêng, GIỮ MỞ)
ngrok http 9000

# 3. Copy URL ngrok (nếu restart)
# https://abc123.ngrok-free.app
```

**Khi code**:
```bash
# Code như bình thường
# Pre-commit hooks tự động chạy khi commit
git add .
git commit -m "feat: new feature [CRM-001]"
git push
```

**CI tự động chạy**:
1. ✅ Lint + validate specs
2. ✅ Unit tests
3. ✅ Coverage checks (80%+ backend, 75%+ frontend)
4. ✅ SonarQube quality analysis
5. ✅ Security scans (OWASP + npm audit)
6. ✅ Build + Docker images
7. ✅ Deploy

**Xem kết quả**:
- GitLab: CI/CD > Pipelines
- SonarQube: `https://abc123.ngrok-free.app` > Projects

### 🔄 Khi Tắt Máy/Restart

**Ngrok URL sẽ thay đổi**, làm theo:
1. Start SonarQube: `docker-compose up -d`
2. Start ngrok: `ngrok http 9000`
3. **Copy URL mới** từ ngrok
4. **Update GitLab CI variable**:
   - GitLab > Settings > CI/CD > Variables
   - Edit `SONAR_HOST_URL` = URL mới

**Hoặc dùng static domain** (recommended):
```bash
# Claim free static domain tại: https://dashboard.ngrok.com/domains
# Ví dụ: your-name-123.ngrok-free.app

# Chạy với static domain (URL không đổi)
ngrok http --domain=your-name-123.ngrok-free.app 9000
```

---

**Ship confidently. 🚀**

**Có vấn đề?** Đọc phần Troubleshooting ở trên hoặc check:
- `docs/automation-setup-guide.md` - Chi tiết đầy đủ
- `docs/WINDOWS-LINE-ENDINGS-FIX.md` - Fix lỗi line endings
