$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    & mvn clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Maven tests/build failed' }
    & java -Xmx2g -jar target/assignment3-graphs-1.0.jar | Tee-Object results/benchmark.log
    if ($LASTEXITCODE -ne 0) { throw 'Benchmark failed' }
    & python scripts/analyze.py
    if ($LASTEXITCODE -ne 0) { throw 'Report generation failed' }
} finally { Pop-Location }
