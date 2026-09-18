[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$frontendDirectory = Join-Path (Split-Path -Parent $PSScriptRoot) 'frontend'
if (-not (Get-Command node.exe -ErrorAction SilentlyContinue)) {
    $toolchainNode = Join-Path $env:LOCALAPPDATA 'Programs\CRS-Toolchain\node-v24.18.0-win-x64'
    if (Test-Path -LiteralPath (Join-Path $toolchainNode 'node.exe')) {
        $env:Path = "$toolchainNode;$env:Path"
    } else { throw 'Node.js is required. Install Node.js 22.12+ or 24 and retry.' }
}
Push-Location $frontendDirectory
try {
    if (-not (Test-Path -LiteralPath 'node_modules')) {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
    }
    Write-Host '[ModelMaestro] Frontend: http://localhost:5173'
    Write-Host '[ModelMaestro] Demo mode works without a backend. Ctrl+C to stop.'
    & npm.cmd run dev
} finally { Pop-Location }
