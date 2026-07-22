param(
  [switch]$WithBot
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $root

$pgBin = Join-Path $root ".postgres\pgsql\bin"
$pgData = Join-Path $root ".postgres-data"
$logDir = Join-Path $root ".logs"
$backendPidFile = Join-Path $logDir "java-backend.pid"
$botPidFile = Join-Path $logDir "java-bot.pid"
$postgresPidFile = Join-Path $logDir "postgres-direct.pid"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Import-DotEnv {
  param([string]$Path)
  if (-not (Test-Path -LiteralPath $Path)) {
    return
  }

  foreach ($line in Get-Content -LiteralPath $Path) {
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith("#") -or -not $trimmed.Contains("=")) {
      continue
    }

    $key, $value = $trimmed.Split("=", 2)
    $key = $key.Trim()
    if (-not $key) {
      continue
    }

    $value = $value.Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
      $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($key, $value, "Process")
  }
}

function Set-DefaultEnv {
  param([string]$Name, [string]$Value)
  if (-not [Environment]::GetEnvironmentVariable($Name, "Process")) {
    [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
  }
}

function Set-LocalEnv {
  param([string]$Name, [string]$Value)
  [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
}

function Test-HttpOk {
  param([string]$Url)
  try {
    $response = Invoke-WebRequest -UseBasicParsing $Url -TimeoutSec 3
    return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
  } catch {
    return $false
  }
}

function Get-LocalJavaExe {
  $javaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "Process")
  if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome "bin\java.exe"))) {
    $javaHome = Join-Path $root ".tools\jdk-21"
    if (-not (Test-Path (Join-Path $javaHome "bin\java.exe"))) {
      & (Join-Path $root "scripts\bootstrap-java.ps1")
    }
  }

  $javaExe = Join-Path $javaHome "bin\java.exe"
  if (-not (Test-Path -LiteralPath $javaExe)) {
    throw "Java 21 was not found. Run scripts\bootstrap-java.ps1 first."
  }

  [Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "Process")
  [Environment]::SetEnvironmentVariable("Path", "$javaHome\bin;$([Environment]::GetEnvironmentVariable("Path", "Process"))", "Process")
  return $javaExe
}

function Build-JavaModule {
  param([string]$Module)

  $runner = Join-Path $root "scripts\run-java-module.ps1"
  $packageArgs = @("-q", "-DskipTests", "-Dskip.unit.tests=true", "package")
  & $runner $Module @packageArgs
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to build Java $Module module."
  }
}

function Get-ModuleJar {
  param([string]$ModulePath)

  $jar = Get-ChildItem -LiteralPath (Join-Path $ModulePath "target") -Filter "*.jar" |
    Where-Object { $_.Name -notlike "*.original" -and $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

  if (-not $jar) {
    throw "No executable jar found in $ModulePath\target."
  }
  return $jar
}

Import-DotEnv (Join-Path $root ".env")

$processPath = [Environment]::GetEnvironmentVariable("Path", "Process")
if (-not $processPath) {
  $processPath = [Environment]::GetEnvironmentVariable("PATH", "Process")
}
[Environment]::SetEnvironmentVariable("PATH", $null, "Process")
[Environment]::SetEnvironmentVariable("Path", $processPath, "Process")

Set-LocalEnv "PORT" "8080"
Set-DefaultEnv "SPRING_PROFILES_ACTIVE" "local"
Set-DefaultEnv "SERVE_FRONTEND" "true"
Set-DefaultEnv "FRONTEND_STATIC_DIR" $root.Path
Set-DefaultEnv "SITE_BASE_URL" "http://localhost:8080"
Set-DefaultEnv "CORS_ALLOWED_ORIGINS" "http://localhost:8080,http://localhost:4177"
Set-DefaultEnv "PUBLIC_UPLOADS_URL" "http://localhost:8080/uploads"
Set-DefaultEnv "FILE_STORAGE_DIR" (Join-Path $root "uploads")
Set-DefaultEnv "JAVA_BACKEND_INTERNAL_URL" "http://localhost:8080"

if (-not (Test-Path (Join-Path $pgBin "pg_ctl.exe"))) {
  throw "Portable PostgreSQL not found. Run the setup steps first."
}

$env:Path = "$pgBin;$env:Path"
$ready = & (Join-Path $pgBin "pg_isready.exe") -h 127.0.0.1 -p 5432 -U postgres 2>$null
if ($LASTEXITCODE -ne 0) {
  $postgresExe = Join-Path $pgBin "postgres.exe"
  if ($env:OS -eq "Windows_NT" -and (Test-Path -LiteralPath $postgresExe)) {
    $postgres = Start-Process `
      -FilePath $postgresExe `
      -ArgumentList "-D", $pgData, "-h", "127.0.0.1", "-p", "5432" `
      -WorkingDirectory $root `
      -RedirectStandardOutput (Join-Path $logDir "postgres-direct.out.log") `
      -RedirectStandardError (Join-Path $logDir "postgres-direct.err.log") `
      -PassThru `
      -WindowStyle Hidden
    Set-Content -LiteralPath $postgresPidFile -Value $postgres.Id -Encoding ASCII
  } else {
    & (Join-Path $pgBin "pg_ctl.exe") -D $pgData -l (Join-Path $pgData "postgres.log") -o "-h 127.0.0.1 -p 5432" start
  }
}

for ($attempt = 1; $attempt -le 20; $attempt++) {
  & (Join-Path $pgBin "pg_isready.exe") -h 127.0.0.1 -p 5432 -U postgres 2>$null | Out-Null
  if ($LASTEXITCODE -eq 0) {
    break
  }
  Start-Sleep -Seconds 1
}
if ($LASTEXITCODE -ne 0) {
  throw "PostgreSQL did not become ready on 127.0.0.1:5432."
}

if (Test-HttpOk "http://localhost:8080/health") {
  Write-Host "Java backend is already responding at http://localhost:8080"
} else {
  if (Test-Path $backendPidFile) {
    $oldPid = Get-Content -LiteralPath $backendPidFile -ErrorAction SilentlyContinue
    if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
      Write-Host "Java backend is already running with PID $oldPid"
      exit 0
    }
  }

  $javaExe = Get-LocalJavaExe
  Build-JavaModule "backend"
  $backendJar = Get-ModuleJar (Join-Path $root "apps\backend-java")
  $backend = Start-Process `
    -FilePath $javaExe `
    -ArgumentList "-jar", $backendJar.FullName `
    -WorkingDirectory $root `
    -RedirectStandardOutput (Join-Path $logDir "java-backend.out.log") `
    -RedirectStandardError (Join-Path $logDir "java-backend.err.log") `
    -PassThru `
    -WindowStyle Hidden
  Set-Content -LiteralPath $backendPidFile -Value $backend.Id -Encoding ASCII
  Write-Host "Java backend started with PID $($backend.Id)"
}

if ($WithBot) {
  if (-not [Environment]::GetEnvironmentVariable("TELEGRAM_BOT_TOKEN", "Process")) {
    Write-Host "Java bot was not started: TELEGRAM_BOT_TOKEN is empty."
  } elseif (Test-Path $botPidFile) {
    $oldBotPid = Get-Content -LiteralPath $botPidFile -ErrorAction SilentlyContinue
    if ($oldBotPid -and (Get-Process -Id $oldBotPid -ErrorAction SilentlyContinue)) {
      Write-Host "Java bot is already running with PID $oldBotPid"
    }
  } else {
    Set-DefaultEnv "BOT_MODE" "polling"
    $javaExe = Get-LocalJavaExe
    Build-JavaModule "bot"
    $botJar = Get-ModuleJar (Join-Path $root "apps\telegram-bot-java")
    $backendPort = [Environment]::GetEnvironmentVariable("PORT", "Process")
    [Environment]::SetEnvironmentVariable("PORT", "8081", "Process")
    $bot = Start-Process `
      -FilePath $javaExe `
      -ArgumentList "-jar", $botJar.FullName `
      -WorkingDirectory $root `
      -RedirectStandardOutput (Join-Path $logDir "java-bot.out.log") `
      -RedirectStandardError (Join-Path $logDir "java-bot.err.log") `
      -PassThru `
      -WindowStyle Hidden
    [Environment]::SetEnvironmentVariable("PORT", $backendPort, "Process")
    Set-Content -LiteralPath $botPidFile -Value $bot.Id -Encoding ASCII
    Write-Host "Java bot started with PID $($bot.Id)"
  }
}

Write-Host "Site/API: http://localhost:8080"
Write-Host "Health: http://localhost:8080/health"
