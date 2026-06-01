# PowerShell launcher: compiles with javac then runs.
# Requires JDK 21+ in PATH.

$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
$out  = Join-Path $root "out"
if (-not (Test-Path $out)) { New-Item -ItemType Directory -Path $out | Out-Null }

$srcRoot = Join-Path $root "src/main/java"
$sources = Get-ChildItem -Recurse -Filter *.java -Path $srcRoot | ForEach-Object { $_.FullName }

Write-Host "Compiling $($sources.Count) source files..."
& javac -d $out --release 21 $sources
if ($LASTEXITCODE -ne 0) { throw "Compilation failed." }

Write-Host "Launching..."
& java -cp $out com.pathfinding.Main
