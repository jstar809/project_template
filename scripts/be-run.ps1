# 백엔드 실행 (http://localhost:8080)
#   .\scripts\be-run.ps1
#   .\scripts\be-run.ps1 -Db mysql
param(
    [ValidateSet('postgresql', 'mysql')]
    [string]$Db = 'postgresql'
)

$root = Split-Path $PSScriptRoot -Parent
$env:DB_VENDOR = $Db
if ($Db -eq 'mysql') { $env:DB_PORT = '3306' } else { $env:DB_PORT = '5432' }

Push-Location (Join-Path $root 'backend')
try {
    .\gradlew.bat bootRun
}
finally {
    Pop-Location
}
