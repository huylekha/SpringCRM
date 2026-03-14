#!/bin/bash

echo "🚀 Starting SonarQube setup..."

# Navigate to sonarqube directory
cd "$(dirname "$0")"

# Start SonarQube
docker-compose up -d

echo "⏳ Waiting for SonarQube to start (this may take 2-3 minutes)..."
echo "   Checking every 5 seconds..."

# Wait for SonarQube to be ready
COUNTER=0
MAX_WAIT=60
until $(curl --output /dev/null --silent --head --fail http://localhost:9000); do
  if [ $COUNTER -eq $MAX_WAIT ]; then
    echo ""
    echo "❌ SonarQube failed to start after 5 minutes"
    echo "   Check logs with: docker-compose logs sonarqube"
    exit 1
  fi
  printf '.'
  sleep 5
  COUNTER=$((COUNTER+1))
done

echo ""
echo "✅ SonarQube is ready at http://localhost:9000"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 Default credentials:"
echo "   Username: admin"
echo "   Password: admin"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔐 IMPORTANT: Change the default password after first login!"
echo ""
echo "📝 Next steps:"
echo "   1. Login to SonarQube at http://localhost:9000"
echo "   2. Change default password when prompted"
echo "   3. Create projects:"
echo "      - Project key: springcrm-backend"
echo "      - Project key: springcrm-frontend"
echo "   4. Generate token: My Account > Security > Generate Tokens"
echo "   5. Add to GitLab CI variables:"
echo "      - SONAR_TOKEN=<generated-token>"
echo "      - SONAR_HOST_URL=http://localhost:9000"
echo ""
echo "🛠️  To stop SonarQube:"
echo "   docker-compose down"
echo ""
echo "🗑️  To remove all data (reset):"
echo "   docker-compose down -v"
echo ""
