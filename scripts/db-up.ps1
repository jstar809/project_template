# DB 컨테이너 실행
#   .\scripts\db-up.ps1              (postgresql)
#   .\scripts\db-up.ps1 -Db mysql
param(
    [ValidateSet('postgresql', 'mysql')]
    [string]$Db = 'postgresql'
)

$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    docker compose --profile $Db up -d
    docker compose ps
}
finally {
    Pop-Location
}
