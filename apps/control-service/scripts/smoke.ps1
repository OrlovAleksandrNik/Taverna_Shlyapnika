$ErrorActionPreference = "Stop"

Push-Location "$PSScriptRoot\..\frontend"
$node = if ($env:CONTROL_NODE_EXE) { $env:CONTROL_NODE_EXE } else { "node" }
& $node scripts\frontend-smoke-test.mjs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Pop-Location

Push-Location "$PSScriptRoot\..\backend"
.\mvnw.cmd -q test
if ($LASTEXITCODE -ne 0) {
  Write-Error "Backend tests failed. Ensure JDK 21 is installed and Maven wrapper can access Maven distribution or a local mvn."
  exit $LASTEXITCODE
}
Pop-Location
