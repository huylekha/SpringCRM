#!/bin/bash

# WSL Auto-Setup Script for SpringCRM
# Run this after fresh Ubuntu installation

echo "🚀 Starting WSL Auto-Setup for SpringCRM..."
echo ""

# Update package lists
echo "📦 Updating package lists..."
sudo apt update -y

# Install essential tools
echo "🔧 Installing essential tools..."
sudo apt install -y \
  git \
  curl \
  wget \
  build-essential \
  ca-certificates \
  software-properties-common

# Install Java 21
echo "☕ Installing Java 21..."
sudo apt install -y openjdk-21-jdk

# Install Maven
echo "🔨 Installing Maven..."
sudo apt install -y maven

# Configure Git
echo "🔐 Configuring Git..."
git config --global core.autocrlf input
git config --global core.eol lf

# Verify installations
echo ""
echo "✅ Verifying installations..."
echo ""

echo "Java version:"
java -version
echo ""

echo "Maven version:"
mvn -version
echo ""

echo "Git version:"
git --version
echo ""

# Navigate to SpringCRM project
echo "📂 Navigating to SpringCRM project..."
cd /mnt/d/Azure/Repo/SpringCRM

# Fix script permissions
echo "🔑 Fixing script permissions..."
chmod +x .husky/pre-commit
chmod +x .husky/lib/*.sh
chmod +x scripts/*.sh

# Test build
echo "🏗️  Testing Maven build..."
cd backend
mvn clean compile -q

if [ $? -eq 0 ]; then
  echo ""
  echo "✅ Maven build successful!"
else
  echo ""
  echo "❌ Maven build failed. Check errors above."
fi

cd ..

# Test pre-commit hook
echo ""
echo "🧪 Testing pre-commit hook..."
bash .husky/pre-commit

echo ""
echo "🎉 WSL setup complete!"
echo ""
echo "Next steps:"
echo "1. Try committing: cd /mnt/d/Azure/Repo/SpringCRM && git status"
echo "2. Run services: cd backend/auth-service && mvn spring-boot:run"
echo "3. Develop happily! 🚀"
