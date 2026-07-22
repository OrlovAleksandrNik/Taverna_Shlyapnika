$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$logDir = Join-Path $root ".logs"
$pidFiles = @(
  (Join-Path $logDir "java-bot.pid"),
  (Join-Path $logDir "java-backend.pid"),
  (Join-Path $logDir "postgres-direct.pid")
)

foreach ($pidFile in $pidFiles) {
  if (-not (Test-Path -LiteralPath $pidFile)) {
    continue
  }

  $pidValue = Get-Content -LiteralPath $pidFile -ErrorAction SilentlyContinue
  if ($pidValue) {
    $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($process) {
      Stop-Process -Id $process.Id -Force
      Write-Host "Stopped $($process.ProcessName) PID $pidValue"
    }
  }
  Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

Write-Host "Java local services stopped."
