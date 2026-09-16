# 백엔드 테스트
#   .\scripts\be-test.ps1            (postgresql)
#   .\scripts\be-test.ps1 -Db mysql
#   .\scripts\be-test.ps1 -All       (두 DB 모두)
param(
    [ValidateSet('postgresql', 'mysql')]
    [string]$Db = 'postgresql',
    [switch]$All
)

$root = Split-Path $PSScriptRoot -Parent
Push-Location (Join-Path $root 'backend')
try {
    $targets = if ($All) { @('postgresql', 'mysql') } else { @($Db) }
    foreach ($t in $targets) {
        Write-Host "`n=== 테스트 대상 DB: $t ===" -ForegroundColor Cyan
        .\gradlew.bat test "-PdbVendor=$t"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "테스트 실패 (DB: $t)" -ForegroundColor Red
            exit $LASTEXITCODE
        }
    }
    Write-Host "`n모든 테스트 통과" -ForegroundColor Green
}
finally {
    Pop-Location
}
