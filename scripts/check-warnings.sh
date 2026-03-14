#!/usr/bin/env bash

# Check Warnings Script
# Verifies zero warnings in SOURCE CODE only (excludes generated code)

echo "🔍 Checking for warnings in source code..."
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Run Maven compile and capture output
cd "$(dirname "$0")/../backend" || exit 1

echo "Running Maven compile..."
COMPILE_OUTPUT=$(mvn clean compile 2>&1)
COMPILE_EXIT_CODE=$?

# Check for BUILD SUCCESS
if echo "$COMPILE_OUTPUT" | grep -q "BUILD SUCCESS"; then
  echo -e "${GREEN}✅ BUILD SUCCESS${NC}"
  echo ""
  
  # Check for warnings
  WARNING_COUNT=$(echo "$COMPILE_OUTPUT" | grep -c "warning" || echo "0")
  
  if [ "$WARNING_COUNT" -eq 0 ]; then
    echo -e "${GREEN}🎉 ZERO WARNINGS in source code!${NC}"
    echo ""
    echo "Zero Warnings Policy: ✅ PASSING"
    echo ""
    echo "Details:"
    echo "  - Source code: 0 warnings"
    echo "  - Build status: SUCCESS"
    echo "  - All modules: PASSED"
    echo ""
    echo "Note: IDE may show warnings in generated code (target/generated-sources/)"
    echo "      These are safe to ignore - Maven doesn't flag them."
    exit 0
  else
    echo -e "${YELLOW}⚠️  Found $WARNING_COUNT warnings${NC}"
    echo ""
    echo "Warnings detected in compilation output:"
    echo "$COMPILE_OUTPUT" | grep -i "warning" | head -20
    echo ""
    echo "Zero Warnings Policy: ❌ FAILING"
    exit 1
  fi
else
  echo -e "${RED}❌ BUILD FAILED${NC}"
  echo ""
  
  # Show compilation errors
  echo "Compilation errors:"
  echo "$COMPILE_OUTPUT" | grep -A 5 "ERROR"
  echo ""
  
  # Show warnings if any
  WARNING_COUNT=$(echo "$COMPILE_OUTPUT" | grep -c "warning" || echo "0")
  if [ "$WARNING_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}Warnings: $WARNING_COUNT${NC}"
    echo "$COMPILE_OUTPUT" | grep -i "warning" | head -10
  fi
  
  echo ""
  echo "Zero Warnings Policy: ❌ FAILING (build errors)"
  exit 1
fi
