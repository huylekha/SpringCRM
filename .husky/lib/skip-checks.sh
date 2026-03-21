#!/usr/bin/env sh

# Emergency Bypass Script
# Allows skipping pre-commit hooks in emergencies with SKIP_HOOKS=1
# Logs all bypasses for audit purposes

# Check if bypass flag is set
if [ "$SKIP_HOOKS" = "1" ]; then
  echo "⚠️  EMERGENCY BYPASS ENABLED - Skipping all pre-commit checks"
  echo "⚠️  Use this only for critical hotfixes!"
  
  # Log bypass to audit file
  AUDIT_LOG=".git/hooks-bypass.log"
  TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
  BRANCH=$(git branch --show-current)
  USER=$(git config user.email)
  
  echo "[$TIMESTAMP] User: $USER | Branch: $BRANCH | SKIP_HOOKS=1" >> "$AUDIT_LOG"
  
  # List staged files for audit
  echo "  Staged files:" >> "$AUDIT_LOG"
  git diff --cached --name-only | sed 's/^/    - /' >> "$AUDIT_LOG"
  echo "" >> "$AUDIT_LOG"
  
  exit 0
fi

# Not bypassing: fall through so pre-commit continues with quality checks
