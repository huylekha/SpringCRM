# Code Review Report Template

**Generated:** {{timestamp}}  
**Scope:** {{scope}} (Full/Backend Only/Frontend Only)  
**Files Changed:** {{fileCount}}  
**Review Mode:** {{mode}} (Auto/Manual)

---

## Executive Summary

{{executiveSummary}}

**Quality Score:** {{totalScore}}/100

- **Architecture Compliance:** {{archScore}}/25
- **Code Quality:** {{qualityScore}}/25
- **Security:** {{securityScore}}/25
- **Test Coverage:** {{testScore}}/25

**Grade:** {{grade}} (Excellent/Good/Acceptable/Needs Work/Major Rework Required)

---

## Changed Services & Files

### Backend Services

{{#if backendChanges}}
{{#each backendChanges}}
#### {{serviceName}} ({{fileCount}} files changed)
{{#each files}}
- `{{path}}` ({{changeType}}: {{linesAdded}}+ / {{linesDeleted}}-)
{{/each}}
{{/each}}
{{else}}
_No backend changes detected_
{{/if}}

### Frontend

{{#if frontendChanges}}
{{#each frontendChanges}}
- `{{path}}` ({{changeType}}: {{linesAdded}}+ / {{linesDeleted}}-)
{{/each}}
{{else}}
_No frontend changes detected_
{{/if}}

### Configuration & Other

{{#if otherChanges}}
{{#each otherChanges}}
- `{{path}}` ({{changeType}})
{{/each}}
{{else}}
_No configuration changes detected_
{{/if}}

---

## Architecture Compliance

| Standard | Status | Details |
|----------|--------|---------|
| Clean Layering (AD-002) | {{cleanLayeringStatus}} | {{cleanLayeringDetails}} |
| Service Boundaries (AD-004) | {{serviceBoundariesStatus}} | {{serviceBoundariesDetails}} |
| Error Handling (AD-007) | {{errorHandlingStatus}} | {{errorHandlingDetails}} |
| Transaction Management (AD-008) | {{transactionStatus}} | {{transactionDetails}} |
| RBAC Enforcement (AD-009) | {{rbacStatus}} | {{rbacDetails}} |
| Database Performance (AD-010) | {{dbPerformanceStatus}} | {{dbPerformanceDetails}} |
| Observability (AD-011) | {{observabilityStatus}} | {{observabilityDetails}} |

**Legend:** ✅ Compliant | ⚠️ Needs Attention | ❌ Non-Compliant | ➖ Not Applicable

---

## Code Quality Issues

{{#if criticalIssues}}
### 🔴 Critical Issues (Must Fix Before Commit)

{{#each criticalIssues}}
#### Issue {{@index}}: {{title}}

**File:** `{{file}}:{{line}}`  
**Category:** {{category}}  
**Severity:** Critical

**Problem:**
{{description}}

**Current Code:**
```{{language}}
{{currentCode}}
```

**Recommended Fix:**
```{{language}}
{{fixedCode}}
```

**Rationale:**
{{rationale}}

**Action Required:**
- [ ] Fix this issue before committing
- [ ] Add tests for the fix
- [ ] Re-run review to verify

---
{{/each}}
{{else}}
✅ **No critical issues detected**
{{/if}}

{{#if warnings}}
### 🟡 Warnings (Should Fix Before Merge)

{{#each warnings}}
#### Warning {{@index}}: {{title}}

**File:** `{{file}}:{{line}}`  
**Category:** {{category}}

**Issue:**
{{description}}

**Current Code:**
```{{language}}
{{currentCode}}
```

**Suggested Improvement:**
```{{language}}
{{suggestedCode}}
```

**Why Fix:**
{{reason}}

---
{{/each}}
{{else}}
✅ **No warnings detected**
{{/if}}

{{#if info}}
### 🔵 Info (Future Improvements)

{{#each info}}
- **{{title}}** (`{{file}}:{{line}}`): {{description}}
{{/each}}
{{else}}
✅ **No improvement suggestions**
{{/if}}

---

## Security Review

### Findings

{{#if securityFindings}}
{{#each securityFindings}}
- **{{severity}}** - {{title}} (`{{file}}:{{line}}`)
  - {{description}}
  - **Fix:** {{fix}}
{{/each}}
{{else}}
✅ **No security issues detected**
{{/if}}

### Security Compliance Checklist

- [{{authCheck}}] Authentication & authorization properly enforced
- [{{validationCheck}}] Input validation complete
- [{{sqlInjectionCheck}}] No SQL injection risks
- [{{xssCheck}}] No XSS vulnerabilities
- [{{secretsCheck}}] Secrets properly managed
- [{{corsCheck}}] CORS configured correctly
- [{{csrfCheck}}] CSRF protection in place

---

## Performance Impact

### Database

**Analysis:**
- **New Queries:** {{newQueriesCount}}
- **Index Coverage:** {{indexCoverage}}
- **N+1 Risk:** {{n1Risk}} (Low/Medium/High)
- **Transaction Duration:** {{transactionDuration}}

{{#if dbIssues}}
**Issues Found:**
{{#each dbIssues}}
- {{description}} (`{{file}}:{{line}}`)
{{/each}}
{{else}}
✅ No database performance issues detected
{{/if}}

### API Performance

**Analysis:**
- **New Endpoints:** {{newEndpointsCount}}
{{#each newEndpoints}}
  - `{{method}} {{path}}` (estimated: {{estimatedResponseTime}}ms)
{{/each}}
- **Modified Endpoints:** {{modifiedEndpointsCount}}
- **Response Time Impact:** {{apiResponseTimeImpact}}

### Frontend Performance

**Analysis:**
- **Bundle Size Delta:** {{bundleSizeDelta}}
- **Component Complexity:** {{componentComplexity}}
- **Render Performance:** {{renderPerformance}}
- **Network Calls:** {{networkCallsImpact}}

{{#if perfIssues}}
**Issues Found:**
{{#each perfIssues}}
- {{description}} (`{{file}}:{{line}}`)
{{/each}}
{{else}}
✅ No frontend performance issues detected
{{/if}}

---

## Test Coverage

### Backend Coverage

{{#if backendCoverage}}
{{#each backendCoverage}}
#### {{serviceName}}
- **Lines:** {{lineCoverage}}% (threshold: 80%)
- **Branches:** {{branchCoverage}}% (threshold: 75%)
- **Status:** {{status}}
{{/each}}
{{else}}
_No backend changes requiring coverage analysis_
{{/if}}

### Frontend Coverage

{{#if frontendCoverage}}
- **Lines:** {{lineCoverage}}% (threshold: 75%)
- **Branches:** {{branchCoverage}}% (threshold: 70%)
- **Functions:** {{functionCoverage}}% (threshold: 70%)
- **Statements:** {{statementCoverage}}% (threshold: 70%)
- **Status:** {{status}}
{{else}}
_No frontend changes requiring coverage analysis_
{{/if}}

### Missing Tests

{{#if missingTests}}
The following files/methods need test coverage:

{{#each missingTests}}
- `{{file}}`:
  {{#each methods}}
  - `{{methodName}}()` - {{reason}}
  {{/each}}
{{/each}}
{{else}}
✅ **All changed code has adequate test coverage**
{{/if}}

---

## Recommendations

### 🔴 Priority 1: Immediate (Before Commit)

{{#if priority1}}
{{#each priority1}}
{{@index}}. **{{title}}**
   - File: `{{file}}:{{line}}`
   - Action: {{action}}
   - Estimated Time: {{estimatedTime}}
{{/each}}
{{else}}
✅ No immediate actions required
{{/if}}

### 🟡 Priority 2: Before Merge

{{#if priority2}}
{{#each priority2}}
{{@index}}. **{{title}}**
   - File: `{{file}}:{{line}}`
   - Action: {{action}}
   - Estimated Time: {{estimatedTime}}
{{/each}}
{{else}}
✅ No pre-merge actions required
{{/if}}

### 🔵 Priority 3: Future Improvements

{{#if priority3}}
{{#each priority3}}
{{@index}}. **{{title}}**
   - Benefit: {{benefit}}
   - Effort: {{effort}}
{{/each}}
{{else}}
_No future improvements identified_
{{/if}}

---

## Next Steps

{{#if hasCriticalIssues}}
### ⚠️ Critical Issues Must Be Addressed

**You cannot proceed to commit until critical issues are fixed.**

**Recommended Actions:**
1. Review each critical issue above
2. Apply the suggested fixes
3. Add tests for the fixes
4. Re-run review: Type `#Review` to verify fixes
5. Proceed to commit once review is clean

**Need Help?**
- Ask me to fix a specific issue: "Fix issue 1 in UserController.java"
- Ask for more details: "Explain the SQL injection risk in detail"
{{else}}
{{#if hasWarnings}}
### ✅ Ready to Commit (with warnings)

**Your code meets minimum quality standards, but has warnings.**

**Options:**
1. **Fix warnings now** (recommended): Address the {{warningCount}} warnings before committing
2. **Commit anyway**: Proceed with commit, fix warnings later
3. **Emergency bypass**: Use `SKIP_HOOKS=1` if this is a hotfix

**To proceed:**
```bash
# Option 1: Fix warnings (recommended)
# Review each warning above and make changes
# Then re-run: #Review

# Option 2: Commit with warnings (will run pre-commit tests)
git commit -m "CRM-XXX: Your commit message"

# Option 3: Emergency bypass (use sparingly)
SKIP_HOOKS=1 git commit -m "hotfix: Your message"
```
{{else}}
### ✅ Ready to Commit

**Excellent work! Your code meets all quality standards.**

**Quality Summary:**
- ✅ No critical issues
- ✅ No warnings
- ✅ Architecture compliant
- ✅ Security verified
- ✅ Test coverage adequate

**Next Steps:**
```bash
# Proceed to commit (strict pre-commit hook will run)
git commit -m "CRM-XXX: Your descriptive commit message"

# Expected pre-commit time:
# - Frontend only: 30-60 seconds
# - Single backend service: 45-90 seconds
# - Multiple services: 1-2 minutes
# - Shared-lib change: 2-3 minutes (tests all services)
```

**Pre-commit will run:**
- Code formatting checks
- Unit tests for changed services
- Coverage validation (80% line, 75% branch)

{{/if}}
{{/if}}

---

## Copy-Paste Fix Commands

{{#if fixCommands}}
For quick fixes, copy and execute these commands:

{{#each fixCommands}}
### Fix: {{title}}

```bash
# {{description}}
{{command}}
```
{{/each}}
{{else}}
_No automated fixes available_
{{/if}}

---

## Review Metadata

- **Review ID:** {{reviewId}}
- **Reviewer:** AI Code Review Bot
- **Review Duration:** {{reviewDuration}}
- **Files Analyzed:** {{filesAnalyzed}}
- **Lines Analyzed:** {{linesAnalyzed}}
- **Standards Version:** {{standardsVersion}}
- **Generated By:** `.cursor/skills/code-review-ai`

---

**Questions or Issues?**
- For help: Ask me to clarify any finding
- To re-run: Type `#Review` after making changes
- For emergency bypass: Use `SKIP_HOOKS=1 git commit` (logged for audit)

---

_This report was generated using the AI Code Review skill based on SpringCRM architecture standards (AD-001 through AD-014). All findings are based on static analysis and best practices._