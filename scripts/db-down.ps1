# DB 컨테이너 종료 (데이터 볼륨은 유지된다)
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    docker compose --profile postgresql --profile mysql down
}
finally {
    Pop-Location
}
