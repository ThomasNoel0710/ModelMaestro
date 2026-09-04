[CmdletBinding()]
param(
    [switch]$SkipTests,
    [switch]$SkipDatabase
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDirectory = $PSScriptRoot
$projectDirectory = Split-Path -Parent $scriptDirectory
$backendDirectory = Join-Path $projectDirectory "backend"
$composeFile = Join-Path $projectDirectory "compose.yml"
$mavenWrapper = Join-Path $backendDirectory "mvnw.cmd"

function Assert-LastCommandSucceeded {
    param([string]$Description)

    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE."
    }
}

function Initialize-Java {
    $javaCommand = Get-Command "java.exe" -ErrorAction SilentlyContinue
    if ($null -ne $javaCommand) {
        return
    }

    if ($env:JAVA_HOME) {
        $javaFromEnvironment = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path -LiteralPath $javaFromEnvironment -PathType Leaf) {
            $env:Path = "$(Split-Path -Parent $javaFromEnvironment);$env:Path"
            return
        }
    }

    $localToolchainRoot = Join-Path $env:LOCALAPPDATA "Programs\CRS-Toolchain\jdk-21"
    if (Test-Path -LiteralPath $localToolchainRoot -PathType Container) {
        $localJdk = Get-ChildItem -LiteralPath $localToolchainRoot -Directory |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "bin\java.exe") } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1

        if ($null -ne $localJdk) {
            $env:JAVA_HOME = $localJdk.FullName
            $env:Path = "$(Join-Path $localJdk.FullName 'bin');$env:Path"
            return
        }
    }

    throw "Java was not found. Install Java 21 or configure JAVA_HOME before running this script."
}

if (-not (Test-Path -LiteralPath $mavenWrapper -PathType Leaf)) {
    throw "Maven Wrapper was not found at $mavenWrapper."
}

Initialize-Java

Write-Host "[ModelMaestro] Java environment"
& java.exe -version
Assert-LastCommandSucceeded "Java version check"

$useLocalDatabase = $false

if (-not $SkipDatabase) {
    $dockerCommand = Get-Command "docker.exe" -ErrorAction SilentlyContinue
    if ($null -eq $dockerCommand) {
        $useLocalDatabase = $true
        Write-Warning "Docker was not found. Using the local H2 development database."
    }
    else {
        Write-Host "[ModelMaestro] Starting PostgreSQL"
        & docker.exe compose -f $composeFile up -d
        Assert-LastCommandSucceeded "PostgreSQL startup"
    }
}

Push-Location $backendDirectory
try {
    Write-Host "[ModelMaestro] Building backend"
    if ($SkipTests) {
        & $mavenWrapper clean package -DskipTests
    }
    else {
        & $mavenWrapper clean verify
    }
    Assert-LastCommandSucceeded "Backend build"

    Write-Host "[ModelMaestro] Starting backend at http://localhost:8080"
    Write-Host "[ModelMaestro] Press Ctrl+C to stop the backend."
    if ($useLocalDatabase) {
        & $mavenWrapper spring-boot:run "-Dspring-boot.run.profiles=local"
    }
    else {
        & $mavenWrapper spring-boot:run
    }
    Assert-LastCommandSucceeded "Backend startup"
}
finally {
    Pop-Location
}
