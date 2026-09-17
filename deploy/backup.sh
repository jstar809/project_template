#!/usr/bin/env bash
#
# DB와 업로드 파일 백업. cron으로 매일 실행한다.
#
#   0 3 * * * /opt/app-prod/backup.sh >> /var/log/app-backup.log 2>&1
#
set -euo pipefail

DEPLOY_PATH="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_PATH"

ENV_FILE=".env.prod"
[ -f "$ENV_FILE" ] || { echo "$ENV_FILE 이 없습니다."; exit 1; }

# .env.prod 는 서버에만 존재하므로 정적 검사 대상이 아니다.
set -a
# shellcheck source=/dev/null
. "./$ENV_FILE"
set +a

BACKUP_PATH="${BACKUP_PATH:-/opt/backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
DB_VENDOR="${DB_VENDOR:-postgresql}"
STAMP="$(date +'%Y%m%d-%H%M%S')"

COMPOSE=(docker compose -f docker-compose.prod.yml --env-file "$ENV_FILE" --profile "$DB_VENDOR")

log() { printf '[%s] %s\n' "$(date +'%Y-%m-%d %H:%M:%S')" "$*"; }

mkdir -p "$BACKUP_PATH"

# ── DB 덤프 (벤더에 따라 명령이 다르다) ───────────────────────
DB_DUMP="$BACKUP_PATH/db-$STAMP.sql.gz"
log "DB 백업 시작 ($DB_VENDOR) → $DB_DUMP"

if [ "$DB_VENDOR" = "mysql" ]; then
    "${COMPOSE[@]}" exec -T mysql \
        mysqldump -u root -p"${DB_ROOT_PASSWORD}" --single-transaction --routines "${DB_NAME}" \
        | gzip > "$DB_DUMP"
else
    "${COMPOSE[@]}" exec -T postgresql \
        pg_dump -U "${DB_USERNAME}" -d "${DB_NAME}" \
        | gzip > "$DB_DUMP"
fi

# ── 업로드 파일 ───────────────────────────────────────────────
UPLOAD_ARCHIVE="$BACKUP_PATH/uploads-$STAMP.tar.gz"
log "업로드 파일 백업 → $UPLOAD_ARCHIVE"
"${COMPOSE[@]}" exec -T backend tar -cf - -C /app uploads | gzip > "$UPLOAD_ARCHIVE"

# ── 오래된 백업 정리 ──────────────────────────────────────────
log "${RETENTION_DAYS}일 지난 백업 삭제"
find "$BACKUP_PATH" -name 'db-*.sql.gz' -mtime "+$RETENTION_DAYS" -delete
find "$BACKUP_PATH" -name 'uploads-*.tar.gz' -mtime "+$RETENTION_DAYS" -delete

log "백업 완료"
find "$BACKUP_PATH" -maxdepth 1 -type f -newermt '-1 day' -exec ls -lh {} +
