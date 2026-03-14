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
