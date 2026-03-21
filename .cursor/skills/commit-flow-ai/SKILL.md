# Commit Flow AI Skill

**Version:** 1.0  
**Skill Type:** Quality Assurance / Git Workflow

## Purpose

Chạy toàn bộ flow verify chất lượng code theo pre-commit (spotless, compile, test, coverage, review) rồi mới cho commit. Trigger khi user gõ `#Commit`.

## Trigger

User gõ `#Commit` trong chat.

## Workflow

### Phase 1: Detect Changes

Chạy `git status --porcelain`, `git diff --cached`, `git diff` để xác định:
- Backend thay đổi: `backend/shared-lib`, `backend/auth-service`, `backend/crm-service`, `backend/api-gateway`
- Frontend thay đổi: `frontend/**`
- Nếu shared-lib thay đổi → coi như cả 3 backend service đều affected

### Phase 2: Backend Auto-Fix & Verify

Nếu có backend changes:

1. **Spotless:** `cd backend && mvn spotless:apply`
   - Tự động format code
   - Nếu có thay đổi sau spotless → stage lại

2. **Compile:** `mvn compile -q`
   - Nếu fail → cố gắng fix (unused imports, syntax) hoặc báo user

3. **Verify per service** (theo detect-changes logic):
   - auth-service: `mvn verify -pl auth-service -am -q`
   - crm-service: `mvn verify -pl crm-service -am -q`
   - api-gateway: `mvn verify -pl api-gateway -am -q`
   - verify = test + jacoco-check (80% line, 75% branch) + spotbugs
   - Nếu fail → báo lỗi, không commit. User hoặc AI fix rồi chạy lại #Commit

### Phase 3: Frontend Verify

Nếu có frontend changes:

1. **Lint:** `cd frontend && npx lint-staged`
2. **Tests:** `npm run test:coverage -- --run`
   - Nếu fail → báo user, không commit

### Phase 4: Code Review

Load logic từ `code-review-ai` skill:
- Phân tích diff (staged + unstaged)
- Kiểm tra compliance (architecture, security, null safety)
- Nếu có **critical issues** → dừng, liệt kê, không commit

### Phase 5: Commit

Nếu tất cả pass:
1. Hỏi user commit message (hoặc dùng message user đã cung cấp trong prompt)
2. `git add` các file đã sửa (nếu cần)
3. `git commit -m "..."` — pre-commit hook sẽ chạy và phải pass

**Quan trọng:** Không dùng `SKIP_HOOKS=1`. Pre-commit phải chạy để đảm bảo chất lượng.

## Auto-Fix vs Manual

| Bước        | Auto-fix                          | Manual (báo user)          |
|-------------|-----------------------------------|----------------------------|
| Spotless    | Có                                | -                          |
| lint-staged | Có (ESLint --fix, Prettier)       | -                          |
| Compile fail| Thử fix imports, syntax đơn giản  | Logic phức tạp             |
| Test fail   | -                                 | Báo, user fix              |
| Coverage    | -                                 | Báo, user viết thêm test   |

## Guardrails

- Không bypass pre-commit
- Nếu critical issues từ review → không commit
- Commit message nên theo format: `feat(module): description [FEATURE-ID]` hoặc `fix/chore/docs`
