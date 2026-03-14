# AI Code Review Skill

**Version:** 1.0  
**Last Updated:** 2026-03-14  
**Skill Type:** Quality Assurance / Code Review

## Purpose

This skill performs comprehensive AI-driven code review of Git changes, analyzing architecture compliance, code quality, security, performance, and test coverage. It can be triggered automatically after coding sessions or manually with `#Review`.

## Trigger Conditions

### Automatic Trigger
- After AI completes a coding task (detected when assistant's last action was file modification)
- User can disable auto-review by setting `AUTO_REVIEW=false` in environment

### Manual Trigger
- User types `#Review` - full review of all staged and unstaged changes
- User types `#ReviewBE` - review backend changes only
- User types `#ReviewFE` - review frontend changes only

## Skill Activation

When this skill is invoked, immediately perform the following workflow:

## Phase 1: Change Detection

### 1.1 Gather Git Changes

Run these commands in parallel:
- `git status --porcelain` - get modified/staged files
- `git diff HEAD` - get unstaged changes
- `git diff --cached` - get staged changes
- `git log -1 --oneline` - get last commit message

### 1.2 Categorize Changes

Organize changed files into categories:

```typescript
interface ChangeCategories {
  backend: {
    authService: string[];      // backend/auth-service/**
    crmService: string[];        // backend/crm-service/**
    apiGateway: string[];        // backend/api-gateway/**
    sharedLib: string[];         // backend/shared-lib/**
  };
  frontend: string[];            // frontend/**
  config: string[];              // *.yml, *.properties, Dockerfile
  docs: string[];                // docs/**, *.md
  tests: string[];               // **/*Test.java, **/*.test.ts
}
```

### 1.3 Identify Change Type

For each file, classify the change:
- **New Feature**: New files, new classes/functions
- **Bug Fix**: Fixes to existing logic
- **Refactor**: Restructuring without behavior change
- **Test**: Test file additions/modifications
- **Config**: Configuration changes
- **Documentation**: README, docs changes

## Phase 2: Compliance Analysis

### 2.1 Load Architecture Standards

Read and internalize these files:
- `docs/summary.md` - Architecture Decisions (AD-001..014)
- `.cursor/rules/spring-architecture.mdc` - Backend architecture rules
- `.cursor/rules/code-review-checklist.mdc` - Review checklist
- `.cursor/rules/spring-security.mdc` - Security patterns
- `.cursor/rules/spring-error-handling.mdc` - Error handling standards
- `.cursor/rules/spring-db-performance.mdc` - Database performance rules

### 2.2 Compliance Matrix

For each changed file, check against applicable standards:

**Backend Services (auth-service, crm-service, api-gateway)**:
- [ ] Clean layering (Controller → Service → Repository)
- [ ] Proper exception handling (BusinessException, ErrorCode enum)
- [ ] Input validation (@Valid, @NotNull, @NotBlank)
- [ ] Transaction boundaries (@Transactional on service methods)
- [ ] No N+1 queries (check for EntityGraph, JOIN FETCH)
- [ ] Proper pagination (Pageable parameter, Page return type)
- [ ] RBAC enforcement (@PreAuthorize with roles)
- [ ] Structured logging (log.info/warn/error with context)
- [ ] Null safety (Optional<T>, Objects.requireNonNull)
- [ ] No hardcoded strings (ErrorCode enum, Constants class)

**Frontend (Next.js/React)**:
- [ ] Feature-based folder structure
- [ ] Proper hook usage (useQuery, useMutation)
- [ ] Form validation (react-hook-form + zod)
- [ ] Error handling (parseApiError, showErrorToast)
- [ ] Type safety (no `any`, proper interfaces)
- [ ] Accessibility (semantic HTML, ARIA labels)
- [ ] Performance (React.memo, useMemo for expensive ops)
- [ ] Internationalization (useTranslations for all text)

**Shared Standards**:
- [ ] Test coverage for new/changed logic
- [ ] No console.log / System.out.println in production code
- [ ] Proper comments (only for non-obvious logic)
- [ ] No TODOs without ticket references

## Phase 3: Code Quality Analysis

### 3.1 Critical Issues (Must Fix)

- **Zero warnings violations**: Compiler warnings, ESLint warnings
- **Security vulnerabilities**: SQL injection, XSS, CSRF
- **Data integrity**: Missing transactions, race conditions
- **Null pointer risks**: Dereferencing without checks
- **Resource leaks**: Unclosed connections, streams
- **Performance killers**: N+1 queries, missing indexes
- **Broken error handling**: Empty catch blocks, swallowed exceptions

**Zero Warnings Policy:**
- Backend: All Java compiler warnings must be fixed (enforced by `-Werror`)
- Frontend: All ESLint warnings must be fixed (enforced by `--max-warnings 0`)
- No `@SuppressWarnings` or `@ts-ignore` without clear justification comment

### 3.2 Warnings (Should Fix)

- **Code smells**: Long methods (>50 lines), God classes
- **Duplication**: Copy-pasted logic across files
- **Weak typing**: Using `any` in TypeScript, Object in Java
- **Missing validation**: Unchecked external input
- **Poor naming**: Unclear variable/method names
- **Test gaps**: Changed logic without corresponding tests

### 3.3 Info (Nice to Have)

- **Refactoring opportunities**: Extract method, simplify conditionals
- **Performance optimizations**: Caching, lazy loading
- **Documentation**: Missing JavaDoc/TSDoc
- **Code style**: Minor formatting inconsistencies

## Phase 4: Security Review

### 4.1 Backend Security Checklist

- [ ] All endpoints have `@PreAuthorize` or `@Secured`
- [ ] Input validation on all DTOs (@Valid, @NotNull)
- [ ] No raw SQL queries (use JPQL or Criteria API)
- [ ] Passwords/secrets use `@JsonIgnore` and are hashed
- [ ] File uploads validate type and size
- [ ] Rate limiting on public endpoints
- [ ] CORS configured correctly (no `allowedOrigins = "*"`)

### 4.2 Frontend Security Checklist

- [ ] No hardcoded API keys or tokens
- [ ] XSS prevention (use dangerouslySetInnerHTML sparingly)
- [ ] CSRF tokens included in forms
- [ ] Sensitive data not logged to console
- [ ] Authentication tokens stored securely (httpOnly cookies)

## Phase 5: Performance Impact

### 5.1 Database Impact

- Check for new queries: Are they indexed?
- Check for N+1: Are relationships fetched efficiently?
- Check for bulk operations: Using batch updates?
- Check for transactions: Proper isolation levels?

### 5.2 API Impact

- New endpoints: Response time acceptable?
- Changed queries: Execution plan optimized?
- New dependencies: Bundle size impact?
- Caching: Are results cacheable?

### 5.3 Frontend Impact

- New components: Bundle size increase?
- New dependencies: Tree-shakeable?
- Rendering: Any expensive re-renders?
- Network: Minimizing API calls?

## Phase 6: Test Coverage Analysis

### 6.1 Backend Tests

For each changed service class:
- Check if corresponding test exists
- Check if new methods have tests
- Check test quality (mocks, assertions, edge cases)
- Verify integration tests if repository changed

### 6.2 Frontend Tests

For each changed component:
- Check if component test exists
- Check for user interaction tests
- Check for error state tests
- Check for loading state tests

## Phase 7: Generate Report

### 7.1 Report Structure

```markdown
# Code Review Report

**Generated:** [timestamp]
**Scope:** [BE/FE/Full]
**Files Changed:** [count]
**Review Mode:** [Auto/Manual]

---

## Executive Summary

[2-3 sentence overview of changes and overall quality]

**Quality Score:** [score]/100
- Architecture Compliance: [score]/25
- Code Quality: [score]/25
- Security: [score]/25
- Test Coverage: [score]/25

---

## Changed Services & Files

### Backend Services
- **auth-service** ([count] files)
  - [list files]
- **crm-service** ([count] files)
  - [list files]
- **api-gateway** ([count] files)
  - [list files]

### Frontend
- [list files with categories: components, services, utils, etc.]

---

## Architecture Compliance

| Standard | Status | Details |
|----------|--------|---------|
| Clean Layering | ✅/⚠️/❌ | [details] |
| Error Handling | ✅/⚠️/❌ | [details] |
| RBAC Patterns | ✅/⚠️/❌ | [details] |
| [etc.] | ✅/⚠️/❌ | [details] |

---

## Code Quality Issues

### 🔴 Critical (Must Fix Before Commit)

#### Issue 1: [Title]
**File:** [filename]:[line]
**Category:** [Security/Data Integrity/Null Safety/etc.]

**Problem:**
[Description of the issue]

**Code:**
\```java
[problematic code snippet]
\```

**Fix:**
\```java
[corrected code snippet]
\```

**Rationale:** [Why this is critical]

---

### 🟡 Warnings (Should Fix)

[Same structure as Critical]

---

### 🔵 Info (Improvements)

[Same structure]

---

## Security Review

### Findings
- [List security findings with severity]

### Compliance Status
- [ ] Input validation complete
- [ ] Authorization checks present
- [ ] No SQL injection risks
- [ ] No XSS vulnerabilities
- [ ] Secrets properly handled

---

## Performance Impact

### Database
- **New Queries:** [count]
- **Index Coverage:** [assessment]
- **N+1 Risk:** [Low/Medium/High]

### API
- **New Endpoints:** [list]
- **Response Time Impact:** [assessment]

### Frontend
- **Bundle Size Delta:** [+X KB]
- **Render Performance:** [assessment]

---

## Test Coverage

### Backend Coverage
- **auth-service:** [X]% lines, [Y]% branches
- **crm-service:** [X]% lines, [Y]% branches
- **api-gateway:** [X]% lines, [Y]% branches

### Frontend Coverage
- **Overall:** [X]% lines, [Y]% branches, [Z]% functions

### Missing Tests
- [List files/methods that need tests]

---

## Recommendations

### Priority 1 (Immediate)
1. [Critical fix 1]
2. [Critical fix 2]

### Priority 2 (Before Merge)
1. [Warning fix 1]
2. [Warning fix 2]

### Priority 3 (Future Improvements)
1. [Enhancement 1]
2. [Enhancement 2]

---

## Next Steps

**If Critical Issues Exist:**
1. Fix critical issues immediately
2. Re-run review with `#Review` to verify fixes
3. Proceed to commit when clean

**If Only Warnings/Info:**
1. Review warnings and decide which to fix
2. Update code as needed
3. Proceed to commit when satisfied

**Emergency Bypass (Use Sparingly):**
\```bash
SKIP_HOOKS=1 git commit -m "hotfix: [description]"
\```

---

## Copy-Paste Fix Commands

For quick fixes, copy these commands:

\```bash
# Fix issue in [file]
# [description]
[command or code snippet to paste]
\```
```

### 7.2 Report Delivery

- Display full report to user
- Save report to `.cursor/reviews/[timestamp]-review.md`
- If critical issues found, suggest specific fixes
- If clean, congratulate and suggest proceeding to commit

## Phase 8: User Interaction

### 8.1 After Report Generation

Ask user:
```
Code review complete! Found:
- 🔴 [X] critical issues
- 🟡 [Y] warnings
- 🔵 [Z] improvements

What would you like to do?
1. Fix critical issues (I'll help)
2. View specific issue details
3. Proceed anyway (use SKIP_HOOKS=1)
4. Re-run review after changes
```

### 8.2 Handling User Response

**Option 1: Fix Issues**
- User describes which issue to fix
- Read relevant file
- Apply fix
- Re-run review to verify

**Option 2: View Details**
- Show detailed analysis of specific issue
- Provide code examples

**Option 3: Proceed Anyway**
- Remind about SKIP_HOOKS=1 flag
- Explain risks
- Log decision in review report

**Option 4: Re-run Review**
- Wait for user to make changes
- Re-run full review workflow

## Error Handling

### If Git Commands Fail
- Check if in Git repository
- Check for unstaged/uncommitted changes
- Provide clear error message with fix

### If Standards Files Missing
- Use built-in knowledge of Spring Boot and React best practices
- Note in report that standards files were not found
- Continue with review

### If No Changes Detected
- Report "No changes to review"
- Suggest running `git status` to verify

## Performance Considerations

- **Large Changesets**: If >50 files changed, focus on high-risk areas first
- **Binary Files**: Skip review of images, compiled files
- **Generated Code**: Note and skip generated files (e.g., Lombok generated methods)
- **Parallel Analysis**: Read multiple files concurrently when possible

## Integration with Workflow

This skill integrates with:
- `.husky/pre-commit` - Pre-commit hook respects review findings
- `.gitlab-ci.yml` - CI pipeline runs similar checks
- `docs/automation/QUICKSTART-AUTOMATION.md` - Documented workflow

## Success Metrics

- Review completion time: <2 minutes for typical changesets
- False positive rate: <10%
- Critical issue detection: 100% (no critical issues reach production)
- User satisfaction: Developer trusts and acts on recommendations

## Example Usage

### Automatic Review (After Coding)
```
[AI finishes implementing feature]
[Auto-review triggers]

"I've completed the user registration feature. Let me review the changes..."

[Generates and displays report]

"Found 1 critical issue in UserController.java - missing input validation. 
Should I fix it now?"
```

### Manual Review
```
User: "#Review"

"Analyzing all Git changes..."
[Generates report]
"Review complete! All checks passed ✅. Ready to commit."
```

### Backend-Only Review
```
User: "#ReviewBE"

"Reviewing backend changes only..."
[Analyzes only backend services]
[Generates focused report]
```

## Maintenance

- **Update Standards**: When architecture decisions change, update compliance checks
- **Add New Checks**: As patterns emerge, add to checklist
- **Refine Scoring**: Adjust quality score weights based on real-world impact
- **Collect Feedback**: Track which issues were false positives, improve detection

---

## Implementation Notes

When implementing this skill:
1. Always read `code-review-checklist.mdc` first for latest standards
2. Prioritize security and data integrity issues
3. Be specific in recommendations (file:line, code snippets)
4. Make report actionable (copy-paste fixes when possible)
5. Respect user's decision to proceed despite warnings
6. Log all reviews for continuous improvement