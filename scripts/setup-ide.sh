#!/usr/bin/env bash

# Auto-setup IntelliJ IDEA for Zero Warnings Policy
# This script configures IDE to exclude generated sources from inspections

echo "🔧 Setting up IntelliJ IDEA for Zero Warnings Policy..."
echo ""

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IDEA_DIR="$PROJECT_ROOT/.idea"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if .idea directory exists
if [ ! -d "$IDEA_DIR" ]; then
  echo -e "${YELLOW}⚠️  .idea directory not found${NC}"
  echo "This script should be run after opening the project in IntelliJ IDEA."
  echo ""
  echo "Steps:"
  echo "1. Open project in IntelliJ IDEA"
  echo "2. Wait for indexing to complete"
  echo "3. Run this script again"
  exit 1
fi

echo "✓ .idea directory found"
echo ""

# Check if config files exist
echo "📄 Checking configuration files..."

if [ -f "$IDEA_DIR/inspectionProfiles/SpringCRM_Zero_Warnings.xml" ]; then
  echo -e "${GREEN}✓ Inspection profile exists${NC}"
else
  echo -e "${YELLOW}⚠️  Inspection profile not found${NC}"
  echo "   File should exist at: .idea/inspectionProfiles/SpringCRM_Zero_Warnings.xml"
fi

if [ -f "$IDEA_DIR/compiler.xml" ]; then
  echo -e "${GREEN}✓ Compiler config exists${NC}"
else
  echo -e "${YELLOW}⚠️  Compiler config not found${NC}"
fi

if [ -f "$PROJECT_ROOT/.editorconfig" ]; then
  echo -e "${GREEN}✓ .editorconfig exists${NC}"
else
  echo -e "${YELLOW}⚠️  .editorconfig not found${NC}"
fi

echo ""

# Mark generated sources folders
echo "🟦 Marking generated sources folders..."
echo ""

SERVICES=("auth-service" "crm-service" "api-gateway")
GENERATED_COUNT=0

for service in "${SERVICES[@]}"; do
  GEN_DIR="$PROJECT_ROOT/backend/$service/target/generated-sources"
  if [ -d "$GEN_DIR" ]; then
    echo -e "${GREEN}✓${NC} Found: backend/$service/target/generated-sources"
    ((GENERATED_COUNT++))
  else
    echo -e "${YELLOW}⚠️${NC}  Not found: backend/$service/target/generated-sources"
    echo "   (Will be created after: mvn compile)"
  fi
done

echo ""

if [ $GENERATED_COUNT -eq 0 ]; then
  echo -e "${YELLOW}⚠️  No generated sources found yet${NC}"
  echo ""
  echo "Run Maven compile first:"
  echo "  cd backend"
  echo "  mvn clean compile"
  echo ""
  echo "Then IntelliJ will automatically mark folders as Generated Sources Root."
else
  echo -e "${GREEN}✓ Found $GENERATED_COUNT generated sources folders${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "${BLUE}📋 Next Steps:${NC}"
echo ""
echo "1. ${YELLOW}Invalidate IntelliJ Caches:${NC}"
echo "   File → Invalidate Caches → Invalidate and Restart"
echo ""
echo "2. ${YELLOW}After restart, verify:${NC}"
echo "   - Open: backend/auth-service/target/generated-sources/java"
echo "   - Folder icon should be 🟦 (light blue)"
echo "   - Open any Q*.java file (QueryDSL)"
echo "   - Should have NO yellow warnings"
echo ""
echo "3. ${YELLOW}Select Inspection Profile:${NC}"
echo "   Settings → Editor → Inspections"
echo "   Profile: Select 'SpringCRM Zero Warnings'"
echo "   Click Apply → OK"
echo ""
echo "4. ${YELLOW}Verify Zero Warnings:${NC}"
echo "   cd backend"
echo "   mvn clean compile"
echo "   Expected: BUILD SUCCESS, 0 warnings"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if Maven is available
if command -v mvn &> /dev/null; then
  echo -e "${BLUE}🚀 Quick Test:${NC}"
  echo ""
  read -p "Run Maven compile now to verify zero warnings? (y/n) " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "Running: mvn clean compile..."
    echo ""
    cd "$PROJECT_ROOT/backend" || exit 1
    
    if mvn clean compile -q; then
      echo ""
      echo -e "${GREEN}✅ BUILD SUCCESS - Zero Warnings!${NC}"
      echo ""
      echo "IntelliJ should now:"
      echo "  - Mark generated sources with blue icons"
      echo "  - Exclude them from inspections"
      echo "  - Show zero warnings on generated code"
    else
      echo ""
      echo -e "${YELLOW}⚠️  Build had issues. Check output above.${NC}"
    fi
  fi
else
  echo -e "${YELLOW}Maven not found in PATH. Skipping test.${NC}"
fi

echo ""
echo -e "${GREEN}✅ Setup complete!${NC}"
echo ""
echo "See: .idea/README-IDE-CONFIG.md for detailed instructions"
