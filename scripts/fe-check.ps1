# 프론트 검사: lint + typecheck + build
$root = Split-Path $PSScriptRoot -Parent
Push-Location (Join-Path $root 'frontend')
try {
    npm run lint; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    npm run typecheck; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    npm run build; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "`n프론트 검사 통과" -ForegroundColor Green
}
finally {
    Pop-Location
}
