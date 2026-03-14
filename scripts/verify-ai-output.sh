#!/bin/bash

SPEC_FILE=$1
IMPL_DIR=$2

if [ -z "$SPEC_FILE" ] || [ -z "$IMPL_DIR" ]; then
  echo "Usage: $0 <spec-file.md> <implementation-directory>"
  exit 1
fi

echo "🔍 Verifying AI-generated output against spec..."
echo "   Spec: $SPEC_FILE"
echo "   Implementation: $IMPL_DIR"
echo ""

# Extract AI Output Expectations section (Section 17)
if ! grep -q "## 17. AI Output Expectations" "$SPEC_FILE"; then
  echo "⚠️  Spec doesn't have Section 17 (AI Output Expectations)"
  echo "   Cannot verify output without checklist"
  exit 0
fi

# Extract expected file list from checklist
EXPECTED_FILES=$(sed -n '/## 17. AI Output Expectations/,/## /p' "$SPEC_FILE" \
  | grep "- \[ \]" \
  | sed 's/.*- \[ \] //' \
  | grep -E '\.(java|ts|tsx|sql|md)$')

if [ -z "$EXPECTED_FILES" ]; then
  echo "⚠️  No files listed in AI Output Expectations checklist"
  exit 0
fi

# Check each expected file
missing_count=0
found_count=0
total_count=0

echo "Expected files from spec:"
echo ""

while IFS= read -r file; do
  ((total_count++))
  
  # Clean filename (remove parentheses and extra text)
  clean_file=$(echo "$file" | sed 's/ (.*//' | xargs)
  
  # Search for file in implementation directory
  if find "$IMPL_DIR" -name "$clean_file" -type f 2>/dev/null | grep -q .; then
    echo "  ✅ Found: $clean_file"
    ((found_count++))
  else
    echo "  ❌ Missing: $clean_file"
    ((missing_count++))
  fi
done <<< "$EXPECTED_FILES"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Verification Results:"
echo "   Total expected: $total_count files"
echo "   Found: $found_count files"
echo "   Missing: $missing_count files"
echo "   Coverage: $(( found_count * 100 / total_count ))%"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $missing_count -gt 0 ]; then
  echo ""
  echo "⚠️  AI output incomplete"
  echo "   Review missing files and complete implementation"
  echo ""
  exit 1
fi

echo ""
echo "✅ AI output verification passed!"
echo ""
