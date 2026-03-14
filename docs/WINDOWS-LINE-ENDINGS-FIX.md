# Quick Fix: Line Endings Issue on Windows

## Problem
Shell scripts (`.sh` files) created on Windows may have CRLF line endings (`\r\n`) instead of Unix LF line endings (`\n`), causing errors like:

```bash
setup.sh: line 2: $'\r': command not found
```

## Solution

### Option 1: Use Git Bash (Recommended)
Git Bash automatically handles line endings correctly.

```bash
# Run from Git Bash
cd devops/sonarqube
bash setup.sh
```

### Option 2: Use WSL (Windows Subsystem for Linux)
```bash
# From WSL terminal
cd /mnt/d/Azure/Repo/SpringCRM/devops/sonarqube
bash setup.sh
```

### Option 3: Convert Line Endings Manually
If you need to use PowerShell/CMD:

```bash
# Install dos2unix (if not available)
# Via Chocolatey: choco install dos2unix
# Via Git for Windows: should be included

# Convert all shell scripts
dos2unix devops/sonarqube/setup.sh
dos2unix scripts/validate-spec.sh
dos2unix scripts/verify-ai-output.sh
dos2unix .husky/pre-commit
dos2unix .husky/commit-msg
dos2unix frontend/.husky/pre-commit
```

### Option 4: Fix with Git (One-time)
```bash
# This will normalize all files according to .gitattributes
git add --renormalize .
git commit -m "fix: normalize line endings"
```

## Prevention

We've added `.gitattributes` to the repository with these rules:

```
*.sh text eol=lf
```

This tells Git to:
- Always checkout `.sh` files with LF endings (Unix style)
- Convert CRLF to LF on commit automatically

**After pulling this change**, Git will automatically handle line endings correctly for all shell scripts.

## Verify Line Endings

Check if a file has CRLF:
```bash
# Git Bash or WSL
file setup.sh

# PowerShell
(Get-Content setup.sh -Raw) -match "`r`n"
```

## Docker Compose Note

If you're running Docker on Windows, make sure Docker Desktop is installed and running before executing `setup.sh`.

```bash
# Check Docker is running
docker --version
docker-compose --version

# Check Docker daemon
docker ps
```

## WSL Integration

For best experience on Windows:

1. **Install WSL2**:
   ```powershell
   wsl --install
   ```

2. **Clone repo in WSL** (optional but recommended):
   ```bash
   cd ~
   git clone <your-repo-url>
   cd SpringCRM
   ```

3. **Run scripts from WSL**:
   All shell scripts will work natively without line ending issues.

## Quick Workaround (If Nothing Else Works)

Run setup manually:
```bash
# Navigate to sonarqube directory
cd devops/sonarqube

# Start Docker Compose
docker-compose up -d

# Wait for startup (2-3 minutes)
# Check status
docker-compose ps

# Access SonarQube
# Open http://localhost:9000 in browser
```

## Summary

**Best practice for Windows users**:
1. Use Git Bash (comes with Git for Windows)
2. Or use WSL2 for full Linux environment
3. `.gitattributes` is now configured to prevent future issues

**One-time fix**:
```bash
git add --renormalize .
```

This ensures all future checkouts have correct line endings.
