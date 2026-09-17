#!/usr/bin/env bash
#
# 서버 최초 준비 스크립트 (Ubuntu 24.04 기준).
# 한 번만 root로 실행한다.
#
#   sudo ./server-setup.sh [배포경로]
#   예) sudo ./server-setup.sh /opt/app-prod
#
set -euo pipefail

DEPLOY_PATH="${1:-/opt/app-prod}"
DEPLOY_USER="${DEPLOY_USER:-deploy}"

log() { printf '[setup] %s\n' "$*"; }

[ "$(id -u)" -eq 0 ] || { echo "root로 실행하세요: sudo $0"; exit 1; }

# ── 1. Docker 설치 ────────────────────────────────────────────
if command -v docker >/dev/null 2>&1; then
    log "Docker가 이미 설치되어 있습니다."
else
    log "Docker 설치"
    apt-get update
    apt-get install -y ca-certificates curl gnupg
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
        | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
    # 우분투 코드명(noble 등)을 읽어 저장소 주소를 만든다.
    # shellcheck source=/dev/null
    . /etc/os-release
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $VERSION_CODENAME stable" \
        > /etc/apt/sources.list.d/docker.list
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    systemctl enable --now docker
fi

# ── 2. 배포 사용자 ────────────────────────────────────────────
if id "$DEPLOY_USER" >/dev/null 2>&1; then
    log "사용자 $DEPLOY_USER 가 이미 있습니다."
else
    log "사용자 $DEPLOY_USER 생성"
    useradd --create-home --shell /bin/bash "$DEPLOY_USER"
fi
usermod -aG docker "$DEPLOY_USER"

# ── 3. 배포 디렉터리 ──────────────────────────────────────────
log "배포 디렉터리 준비: $DEPLOY_PATH"
mkdir -p "$DEPLOY_PATH/.deploy" "${BACKUP_PATH:-/opt/backups}"
chown -R "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_PATH" "${BACKUP_PATH:-/opt/backups}"

# ── 4. 방화벽 ─────────────────────────────────────────────────
if command -v ufw >/dev/null 2>&1; then
    log "방화벽 설정 (22, 80, 443만 허용)"
    ufw allow 22/tcp
    ufw allow 80/tcp
    ufw allow 443/tcp
    ufw --force enable
else
    log "ufw가 없어 방화벽 설정을 건너뜁니다."
fi

cat <<EOF

────────────────────────────────────────────────────────────
서버 준비 완료. 다음을 직접 해주세요.

1. 배포용 SSH 공개키 등록
   mkdir -p /home/$DEPLOY_USER/.ssh
   echo "<공개키>" >> /home/$DEPLOY_USER/.ssh/authorized_keys
   chown -R $DEPLOY_USER:$DEPLOY_USER /home/$DEPLOY_USER/.ssh
   chmod 700 /home/$DEPLOY_USER/.ssh && chmod 600 /home/$DEPLOY_USER/.ssh/authorized_keys

2. 배포 파일 배치 ($DEPLOY_PATH)
   docker-compose.prod.yml, Caddyfile, deploy.sh, backup.sh
   (CD 워크플로가 자동으로 복사한다)

3. 비밀값 파일 작성
   cp .env.prod.example $DEPLOY_PATH/.env.prod
   vi $DEPLOY_PATH/.env.prod     # DOMAIN, 이미지 주소, 비밀번호, JWT_SECRET

4. 도메인 DNS의 A 레코드를 이 서버 IP로 지정
   (Caddy가 HTTPS 인증서를 자동 발급하려면 필요하다)

5. 백업 cron 등록
   crontab -u $DEPLOY_USER -e
   0 3 * * * $DEPLOY_PATH/backup.sh >> /var/log/app-backup.log 2>&1
────────────────────────────────────────────────────────────
EOF
