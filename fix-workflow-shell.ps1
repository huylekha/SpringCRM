# Fix GitHub Actions workflow to use bash shell on Windows

$workflowFile = ".github/workflows/local-docker-compose-ci-cd.yml"
$content = Get-Content $workflowFile -Raw

# Add shell: bash before run: | commands (except those already having shell)
$pattern = '(\s+)- name: ([^\n]+)\n(\s+)run: \|'
$replacement = '$1- name: $2' + "`n" + '$3shell: bash' + "`n" + '$3run: |'

$newContent = $content -replace $pattern, $replacement

# Also fix single line run commands that use bash syntax
$pattern2 = '(\s+)run: (.*(?:\$\{|\|\||&&|if \[).*)'
$replacement2 = '$1shell: bash' + "`n" + '$1run: $2'

$newContent = $newContent -replace $pattern2, $replacement2

# Write back
Set-Content $workflowFile $newContent -Encoding UTF8

Write-Host "✅ Fixed workflow shell configuration for Windows runner"