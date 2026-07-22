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

if ($MavenArgs.Count -gt 0 -and ($MavenArgs[0] -eq "--" -or $MavenArgs[0] -eq "--%")) {
  $MavenArgs = if ($MavenArgs.Count -gt 1) { $MavenArgs[1..($MavenArgs.Count - 1)] } else { @() }
}

$MavenArgs = @($MavenArgs | ForEach-Object { $_.Trim() } | Where-Object { $_ })

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

$buildRoot = $repoRoot.Path
$substDrive = $null
if ($IsWindows -or $env:OS -eq "Windows_NT") {
  $hasNonAsciiPath = $buildRoot.ToCharArray() | Where-Object { [int][char]$_ -gt 127 } | Select-Object -First 1
  if ($hasNonAsciiPath -and $env:TAVERNA_DISABLE_SUBST -ne "true") {
    foreach ($candidate in @("T:", "V:", "W:")) {
      if (-not (Test-Path "$candidate\")) {
        & "$env:SystemRoot\System32\cmd.exe" /c "subst $candidate `"$buildRoot`""
        if ($LASTEXITCODE -eq 0 -and (Test-Path "$candidate\")) {
          $substDrive = $candidate
          $buildRoot = "$candidate\"
          break
        }
      }
    }
  }
}

$resolvedJavaHome = (Resolve-Path $javaHome).Path
if ($buildRoot -ne $repoRoot.Path -and $resolvedJavaHome.StartsWith($repoRoot.Path, [StringComparison]::OrdinalIgnoreCase)) {
  $relativeJavaHome = $resolvedJavaHome.Substring($repoRoot.Path.Length).TrimStart("\")
  $resolvedJavaHome = Join-Path $buildRoot $relativeJavaHome
}

$env:JAVA_HOME = $resolvedJavaHome
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$modulePath = switch ($Module) {
  "backend" { Join-Path $buildRoot "apps\backend-java" }
  "bot" { Join-Path $buildRoot "apps\telegram-bot-java" }
}

Push-Location $modulePath
$exitCode = 0
try {
  Write-Host "Running Java $Module module with Maven arguments: $($MavenArgs -join ' ')"
  & ".\mvnw.cmd" @MavenArgs
  $exitCode = $LASTEXITCODE
} finally {
  Pop-Location
  if ($substDrive) {
    & "$env:SystemRoot\System32\cmd.exe" /c "subst $substDrive /d" | Out-Null
  }
}

exit $exitCode
