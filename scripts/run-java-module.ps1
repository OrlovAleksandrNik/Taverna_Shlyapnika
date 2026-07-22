param(
  [Parameter(Mandatory = $true, Position = 0)]
  [ValidateSet("backend", "bot")]
  [string]$Module,

  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$MavenArgs
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..")
$modulePath = switch ($Module) {
  "backend" { Join-Path $repoRoot "apps\backend-java" }
  "bot" { Join-Path $repoRoot "apps\telegram-bot-java" }
}

if ($MavenArgs.Count -gt 0 -and $MavenArgs[0] -eq "--") {
  $MavenArgs = if ($MavenArgs.Count -gt 1) { $MavenArgs[1..($MavenArgs.Count - 1)] } else { @() }
}

if ($MavenArgs.Count -eq 0) {
  $MavenArgs = @("test")
}

$javaHome = $env:JAVA_HOME
if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome "bin\java.exe"))) {
  $localJavaHome = Join-Path $repoRoot ".tools\jdk-21"
  if (-not (Test-Path (Join-Path $localJavaHome "bin\java.exe"))) {
    & (Join-Path $scriptDir "bootstrap-java.ps1")
  }
  $javaHome = $localJavaHome
}

if (-not (Test-Path (Join-Path $javaHome "bin\java.exe"))) {
  throw "Java 21 was not found. Run scripts\bootstrap-java.ps1 first."
}

$env:JAVA_HOME = (Resolve-Path $javaHome).Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Push-Location $modulePath
try {
  & ".\mvnw.cmd" @MavenArgs
  exit $LASTEXITCODE
} finally {
  Pop-Location
}
