#!/bin/bash

SPEC_FILE=$1

if [ -z "$SPEC_FILE" ]; then
  echo "Usage: $0 <spec-file.md>"
  exit 1
fi

if [ ! -f "$SPEC_FILE" ]; then
  echo "❌ Spec file not found: $SPEC_FILE"
  exit 1
fi

echo "🔍 Validating Feature Specification: $SPEC_FILE"
echo ""

# Check all 17 sections exist
REQUIRED_SECTIONS=(
  "## 1. Feature Metadata"
  "## 2. Actors"
  "## 3. Business Context"
  "## 4. Business Flow"
  "## 5. API Contract"
  "## 6. Validation Rules"
  "## 7. Domain Model"
  "## 8. State Machine"
  "## 9. External Integrations"
  "## 10. Event Flow"
  "## 11. Error Codes"
  "## 12. Observability"
  "## 13. Security Requirements"
  "## 14. Performance Requirements"
  "## 15. Frontend UI Specification"
  "## 16. Test Scenarios"
  "## 17. AI Output Expectations"
)

score=0
total=${#REQUIRED_SECTIONS[@]}
missing_sections=()

for section in "${REQUIRED_SECTIONS[@]}"; do
  if grep -q "$section" "$SPEC_FILE"; then
    ((score++))
  else
    missing_sections+=("$section")
  fi
done

echo "📊 Section Completeness: $score/$total"

if [ $score -lt $total ]; then
  echo ""
  echo "❌ Missing sections:"
  for missing in "${missing_sections[@]}"; do
    echo "   - $missing"
  done
  exit 1
fi

# Check for TBD placeholders
TBD_COUNT=$(grep -iE "TBD|to be defined|to be decided" "$SPEC_FILE" | wc -l)
if [ $TBD_COUNT -gt 0 ]; then
  echo ""
  echo "⚠️  Found $TBD_COUNT TBD placeholder(s)"
  echo "   Spec should not contain TBD or 'to be defined'"
  exit 1
fi

# Check for JSON examples in API Contract section
JSON_EXAMPLES=$(sed -n '/## 5. API Contract/,/## 6./p' "$SPEC_FILE" | grep -c '```json')
if [ $JSON_EXAMPLES -lt 2 ]; then
  echo ""
  echo "⚠️  API Contract section should have JSON request/response examples"
  echo "   Found $JSON_EXAMPLES JSON block(s), expected at least 2"
  exit 1
fi

# Check for Architecture Decision references
AD_REFS=$(grep -cE "AD-[0-9]+" "$SPEC_FILE")
if [ $AD_REFS -lt 1 ]; then
  echo ""
  echo "⚠️  Spec should reference Architecture Decisions (AD-001..014)"
  echo "   See docs/summary.md for list of decisions"
fi

# Check for audit fields in Domain Model
AUDIT_FIELDS=$(sed -n '/## 7. Domain Model/,/## 8./p' "$SPEC_FILE" | grep -cE "created_at|created_by|updated_at|updated_by")
if [ $AUDIT_FIELDS -lt 1 ]; then
  echo ""
  echo "⚠️  Domain Model should include audit fields"
  echo "   Required: created_at, created_by, updated_at, updated_by"
fi

echo ""
echo "✅ Feature Specification validation passed!"
echo ""
echo "📋 Next steps:"
echo "   1. Review spec completeness score"
echo "   2. Address any warnings above"
echo "   3. Get spec approved by tech lead"
echo "   4. Implement with: #BE implement $SPEC_FILE"
echo ""
