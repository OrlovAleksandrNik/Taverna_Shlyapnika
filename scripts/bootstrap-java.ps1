param(
  [string]$Version = "21"
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$ToolsDir = Join-Path $RepoRoot ".tools"
$JdkHome = Join-Path $ToolsDir "jdk-$Version"
$JavaExe = Join-Path $JdkHome "bin\java.exe"
$DownloadDir = Join-Path $ToolsDir "downloads"
$Archive = Join-Path $DownloadDir "temurin-jdk-$Version-windows-x64.zip"
$ExtractDir = Join-Path $ToolsDir "jdk-$Version-extract"
$DownloadUrl = "https://api.adoptium.net/v3/binary/latest/$Version/ga/windows/x64/jdk/hotspot/normal/eclipse"

function Assert-InRepo([string]$PathToCheck) {
  $resolved = [System.IO.Path]::GetFullPath($PathToCheck)
  $root = [System.IO.Path]::GetFullPath($RepoRoot)
  if (-not $resolved.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to operate outside repository: $resolved"
  }
}

Assert-InRepo $ToolsDir
Assert-InRepo $JdkHome
Assert-InRepo $DownloadDir
Assert-InRepo $Archive
Assert-InRepo $ExtractDir

if (Test-Path $JavaExe) {
  & $JavaExe -version
  Write-Host "JDK $Version already available at $JdkHome"
  exit 0
}

New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
New-Item -ItemType Directory -Force -Path $DownloadDir | Out-Null

if (-not (Test-Path $Archive)) {
  Write-Host "Downloading Eclipse Temurin JDK $Version..."
  Invoke-WebRequest -Uri $DownloadUrl -OutFile $Archive
}

if (Test-Path $ExtractDir) {
  Assert-InRepo $ExtractDir
  Remove-Item -LiteralPath $ExtractDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $ExtractDir | Out-Null

Expand-Archive -LiteralPath $Archive -DestinationPath $ExtractDir -Force

$ExtractedJava = Get-ChildItem -Path $ExtractDir -Recurse -Filter java.exe | Select-Object -First 1
if (-not $ExtractedJava) {
  throw "Downloaded archive does not contain java.exe"
}

$ExtractedHome = Split-Path (Split-Path $ExtractedJava.FullName -Parent) -Parent
Assert-InRepo $ExtractedHome

if (Test-Path $JdkHome) {
  Assert-InRepo $JdkHome
  Remove-Item -LiteralPath $JdkHome -Recurse -Force
}

Move-Item -LiteralPath $ExtractedHome -Destination $JdkHome

if (Test-Path $ExtractDir) {
  Assert-InRepo $ExtractDir
  Remove-Item -LiteralPath $ExtractDir -Recurse -Force
}

& $JavaExe -version
Write-Host "JDK $Version installed at $JdkHome"
