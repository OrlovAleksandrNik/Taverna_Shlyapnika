$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $root

$pgBin = Join-Path $root ".postgres\pgsql\bin"
$pgData = Join-Path $root ".postgres-data"
$logDir = Join-Path $root ".logs"
$appPidFile = Join-Path $logDir "app.pid"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (-not (Test-Path (Join-Path $pgBin "pg_ctl.exe"))) {
  throw "Portable PostgreSQL not found. Run the setup steps first."
}

$env:Path = "$pgBin;$env:Path"

$ready = & (Join-Path $pgBin "pg_isready.exe") -h 127.0.0.1 -p 5432 -U postgres 2>$null
if ($LASTEXITCODE -ne 0) {
  & (Join-Path $pgBin "pg_ctl.exe") -D $pgData -l (Join-Path $pgData "postgres.log") -o "-h 127.0.0.1 -p 5432" start
}

$nodeCommand = Get-Command node -ErrorAction SilentlyContinue
$node = $null
if ($nodeCommand) {
  $node = $nodeCommand.Source
}
if (-not $node) {
  $node = Join-Path $env:USERPROFILE ".cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe"
}
if (-not (Test-Path $node)) {
  throw "Node.js not found. Install Node.js or run from Codex runtime."
}

$processPath = [Environment]::GetEnvironmentVariable("Path", "Process")
if (-not $processPath) {
  $processPath = [Environment]::GetEnvironmentVariable("PATH", "Process")
}
[Environment]::SetEnvironmentVariable("PATH", $null, "Process")
[Environment]::SetEnvironmentVariable("Path", $processPath, "Process")

try {
  $health = Invoke-WebRequest -UseBasicParsing "http://localhost:4177/health" -TimeoutSec 3
  if ($health.StatusCode -eq 200) {
    Write-Host "Taverna app is already responding at http://localhost:4177"
    exit 0
  }
} catch {
}

if (Test-Path $appPidFile) {
  $oldPid = Get-Content -LiteralPath $appPidFile -ErrorAction SilentlyContinue
  if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
    Write-Host "Taverna app is already running with PID $oldPid"
    exit 0
  }
}

$process = Start-Process -FilePath $node -ArgumentList "dist/index.js" -WorkingDirectory $root -RedirectStandardOutput (Join-Path $logDir "app.out.log") -RedirectStandardError (Join-Path $logDir "app.err.log") -PassThru -WindowStyle Hidden
Set-Content -LiteralPath $appPidFile -Value $process.Id -Encoding ASCII
Write-Host "Taverna app started with PID $($process.Id)"
Write-Host "Site/API: http://localhost:4177"
