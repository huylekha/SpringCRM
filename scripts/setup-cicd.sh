#!/bin/bash
# GitHub Actions CI/CD Quick Setup Script for Linux/Mac
# Run with: sudo ./setup-cicd.sh <github-url> <runner-token> [runner-name]

set -e

GITHUB_URL="$1"
RUNNER_TOKEN="$2"
RUNNER_NAME="${3:-local-runner}"

if [ -z "$GITHUB_URL" ] || [ -z "$RUNNER_TOKEN" ]; then
    echo "❌ Usage: $0 <github-url> <runner-token> [runner-name]"
    echo "Example: $0 https://github.com/username/SpringCRM ghp_xxxxxxxxxxxx my-runner"
    exit 1
fi

echo "🚀 Setting up GitHub Actions Self-Hosted Runner..."

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "❌ This script must be run as root (use sudo)"
    exit 1
fi

# Check Docker
echo "📋 Checking prerequisites..."
if command -v docker >/dev/null 2>&1 && command -v docker-compose >/dev/null 2>&1; then
    echo "✅ Docker and Docker Compose are installed"
else
    echo "❌ Docker or Docker Compose not found. Please install Docker first."
    exit 1
fi

# Create runner directory
RUNNER_DIR="/opt/actions-runner"
if [ -d "$RUNNER_DIR" ]; then
    echo "📁 Removing existing runner directory..."
    rm -rf "$RUNNER_DIR"
fi

echo "📁 Creating runner directory: $RUNNER_DIR"
mkdir -p "$RUNNER_DIR"
cd "$RUNNER_DIR"

# Download runner
echo "⬇️ Downloading GitHub Actions runner..."
RUNNER_URL="https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz"
RUNNER_TAR="actions-runner-linux-x64-2.311.0.tar.gz"

if curl -o "$RUNNER_TAR" -L "$RUNNER_URL"; then
    echo "✅ Runner downloaded successfully"
else
    echo "❌ Failed to download runner"
    exit 1
fi

# Extract runner
echo "📦 Extracting runner..."
if tar xzf "./$RUNNER_TAR"; then
    rm "$RUNNER_TAR"
    echo "✅ Runner extracted successfully"
else
    echo "❌ Failed to extract runner"
    exit 1
fi

# Create runner user
echo "👤 Creating runner user..."
if ! id "runner" &>/dev/null; then
    useradd -m -s /bin/bash runner
    echo "✅ Runner user created"
else
    echo "✅ Runner user already exists"
fi

# Set permissions
chown -R runner:runner "$RUNNER_DIR"

# Configure runner
echo "⚙️ Configuring runner..."
sudo -u runner ./config.sh --url "$GITHUB_URL" --token "$RUNNER_TOKEN" --name "$RUNNER_NAME" --work "_work" --replace

# Install and start service
echo "🔧 Installing runner as systemd service..."
./svc.sh install runner
./svc.sh start

# Verify service
echo "🔍 Verifying runner status..."
if systemctl is-active --quiet actions.runner.*.service; then
    echo "✅ Runner service is running"
else
    echo "⚠️ Runner service may not be running properly. Check manually."
fi

echo ""
echo "🎉 GitHub Actions Runner Setup Complete!"
echo ""
echo "Next steps:"
echo "1. Go to your GitHub repository settings to verify the runner is online"
echo "2. Configure GitHub Secrets (see SETUP-GITHUB-ACTIONS.md)"
echo "3. Make a commit to trigger the CI/CD pipeline"
echo ""
echo "Runner location: $RUNNER_DIR"
echo "Service management: systemctl status actions.runner.*.service"
echo ""