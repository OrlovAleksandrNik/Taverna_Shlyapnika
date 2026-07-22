$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $root

$pgBin = Join-Path $root ".postgres\pgsql\bin"
$pgData = Join-Path $root ".postgres-data"
$appPidFile = Join-Path $root ".logs\app.pid"

if (Test-Path $appPidFile) {
  $pidValue = Get-Content -LiteralPath $appPidFile -ErrorAction SilentlyContinue
  if ($pidValue) {
    $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($process) {
      Stop-Process -Id $process.Id -Force
      Write-Host "Stopped Taverna app PID $pidValue"
    }
  }
  Remove-Item -LiteralPath $appPidFile -Force -ErrorAction SilentlyContinue
}

if (Test-Path (Join-Path $pgBin "pg_ctl.exe")) {
  & (Join-Path $pgBin "pg_ctl.exe") -D $pgData stop -m fast
}
