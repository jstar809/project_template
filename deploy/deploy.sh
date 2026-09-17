#!/usr/bin/env bash
#
# 배포 스크립트. CD 워크플로가 SSH로 이 스크립트를 실행한다.
#
#   ./deploy.sh <이미지태그>
#   예) ./deploy.sh sha-1a2b3c4
#
# 하는 일
#   1. 현재 태그를 기억한다 (롤백용)
#   2. 새 이미지를 받아 교체한다
#   3. 헬스체크한다
#   4. 실패하면 이전 태그로 되돌리고 종료 코드 1로 끝낸다
#
set -euo pipefail

DEPLOY_PATH="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_PATH"

COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env.prod"
STATE_DIR=".deploy"
CURRENT_TAG_FILE="$STATE_DIR/current_tag"

HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-120}"
HEALTH_INTERVAL_SECONDS=5

log() { printf '[%s] %s\n' "$(date +'%Y-%m-%d %H:%M:%S')" "$*"; }
fail() { log "오류: $*"; exit 1; }

[ $# -eq 1 ] || fail "사용법: $0 <이미지태그>"
NEW_TAG="$1"

[ -f "$COMPOSE_FILE" ] || fail "$COMPOSE_FILE 이 없습니다."
[ -f "$ENV_FILE" ] || fail "$ENV_FILE 이 없습니다. .env.prod.example 을 복사해 채우세요."

mkdir -p "$STATE_DIR"

# .env.prod 는 서버에만 존재하므로 정적 검사 대상이 아니다.
set -a
# shellcheck source=/dev/null
. "./$ENV_FILE"
set +a

DB_PROFILE="${DB_VENDOR:-postgresql}"
COMPOSE=(docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" --profile "$DB_PROFILE")

PREVIOUS_TAG=""
[ -f "$CURRENT_TAG_FILE" ] && PREVIOUS_TAG="$(cat "$CURRENT_TAG_FILE")"

log "배포 시작: ${PREVIOUS_TAG:-(이전 없음)} → $NEW_TAG"

# GHCR 패키지가 비공개면 로그인이 필요하다.
if [ -n "${GHCR_READ_TOKEN:-}" ]; then
    log "GHCR 로그인"
    echo "$GHCR_READ_TOKEN" | docker login ghcr.io -u "${GHCR_USER:?GHCR_USER 가 필요합니다}" --password-stdin
fi

# 헬스체크: backend 컨테이너 안에서 스스로를 확인한다.
# 외부(도메인)에서 확인하면 DNS·인증서 문제까지 섞여 원인을 가리기 어렵다.
health_check() {
    local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
    while [ $SECONDS -lt $deadline ]; do
        if "${COMPOSE[@]}" exec -T backend \
            curl -fsS "http://localhost:8080/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
            return 0
        fi
        sleep "$HEALTH_INTERVAL_SECONDS"
    done
    return 1
}

deploy_tag() {
    local tag="$1"
    IMAGE_TAG="$tag" "${COMPOSE[@]}" pull
    IMAGE_TAG="$tag" "${COMPOSE[@]}" up -d --remove-orphans
}

log "새 이미지 받기 및 교체"
deploy_tag "$NEW_TAG"

log "헬스체크 (최대 ${HEALTH_TIMEOUT_SECONDS}초)"
if health_check; then
    echo "$NEW_TAG" > "$CURRENT_TAG_FILE"
    log "배포 성공: $NEW_TAG"
    docker image prune -f >/dev/null 2>&1 || true
    exit 0
fi

log "헬스체크 실패"
"${COMPOSE[@]}" logs --tail=100 backend || true

if [ -z "$PREVIOUS_TAG" ]; then
    fail "되돌릴 이전 배포가 없습니다. 수동 확인이 필요합니다."
fi

log "롤백 시작: $PREVIOUS_TAG 로 되돌립니다"
deploy_tag "$PREVIOUS_TAG"

if health_check; then
    log "롤백 성공: $PREVIOUS_TAG (현재 태그는 그대로 유지)"
else
    log "롤백 후에도 헬스체크가 실패했습니다. 서버를 직접 확인하세요."
fi

# 배포는 실패했으므로 워크플로도 실패로 표시되어야 한다.
exit 1
