# 프론트 개발 서버 (http://localhost:5173)
$root = Split-Path $PSScriptRoot -Parent
Push-Location (Join-Path $root 'frontend')
try {
    if (-not (Test-Path 'node_modules')) { npm install }
    npm run dev
}
finally {
    Pop-Location
}
