# 배포 가이드

GitHub Actions로 이미지를 만들고, SSH로 서버에 배포한다. 헬스체크가 실패하면 자동으로 이전 버전으로 되돌린다.

```text
main 브랜치 push
   ↓
이미지 빌드 → GHCR 푸시 (태그: sha-abc1234, latest)
   ↓
staging 서버 배포 → 헬스체크
   ↓ (성공 시)
production 배포 대기 → 담당자 승인 → 배포 → 헬스체크
```

## 1. 서버 준비 (최초 1회)

### 1-1. 서버 사양
Ubuntu 24.04 LTS, 최소 2 vCPU / 4GB RAM 권장. staging과 production은 서버를 나눠도 되고, 한 서버에서 디렉터리와 도메인만 나눠도 된다.

### 1-2. 준비 스크립트 실행

```bash
# 서버에 접속해서
curl -O https://raw.githubusercontent.com/<사용자>/<레포>/main/deploy/server-setup.sh
chmod +x server-setup.sh
sudo ./server-setup.sh /opt/app-prod
```

스크립트가 하는 일: Docker 설치, `deploy` 사용자 생성, 배포 디렉터리 준비, 방화벽(22·80·443만 허용).

### 1-3. SSH 키 등록

로컬에서 배포 전용 키를 만든다.

```bash
ssh-keygen -t ed25519 -C "github-deploy" -f ~/.ssh/app_deploy -N ""
```

공개키를 서버에 등록한다.

```bash
# 서버에서
mkdir -p /home/deploy/.ssh
echo "<app_deploy.pub 내용>" >> /home/deploy/.ssh/authorized_keys
chown -R deploy:deploy /home/deploy/.ssh
chmod 700 /home/deploy/.ssh && chmod 600 /home/deploy/.ssh/authorized_keys
```

### 1-4. 비밀값 파일

```bash
# 서버의 /opt/app-prod 에서
cp .env.prod.example .env.prod
vi .env.prod
```

반드시 바꿔야 하는 값:

| 값 | 설명 |
|---|---|
| `DOMAIN` | 실제 도메인 (Caddy가 이 도메인으로 인증서를 받는다) |
| `BACKEND_IMAGE`, `FRONTEND_IMAGE` | `ghcr.io/<사용자>/<레포>-backend` 형태 |
| `DB_PASSWORD`, `DB_ROOT_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | `openssl rand -base64 48` 로 생성 |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | 최초 관리자 계정 (생성 후 비워두는 것을 권장) |
| `GHCR_READ_TOKEN` | GHCR 패키지가 비공개일 때만 (PAT, `read:packages` 권한) |

### 1-5. DNS
도메인의 A 레코드를 서버 IP로 지정한다. Caddy가 HTTPS 인증서를 자동 발급하려면 필요하다.

## 2. GitHub 설정

### 2-1. Environments
레포 Settings → Environments에서 두 개를 만든다.

| 이름 | 설정 |
|---|---|
| `staging` | 보호 규칙 없음 (자동 배포) |
| `production` | **Required reviewers**에 승인자 지정 (승인해야 배포됨) |

### 2-2. Environment별 Secrets

| 이름 | 값 | 얻는 방법 |
|---|---|---|
| `SSH_HOST` | 서버 IP 또는 도메인 | |
| `SSH_PORT` | 22 | |
| `SSH_USER` | deploy | |
| `SSH_PRIVATE_KEY` | 개인키 전체 | `cat ~/.ssh/app_deploy` |
| `SSH_KNOWN_HOSTS` | 호스트 키 | `ssh-keyscan -p 22 <서버IP>` |

> `SSH_KNOWN_HOSTS`는 중간자 공격을 막기 위한 것이다. 이걸 생략하고 `StrictHostKeyChecking=no`를 쓰면 안 된다.

### 2-3. Environment별 Variables

| 이름 | 예시 |
|---|---|
| `DEPLOY_PATH` | `/opt/app-prod` (staging은 `/opt/app-staging`) |

### 2-4. 이미지 저장소(GHCR)
`GITHUB_TOKEN`으로 자동 푸시되므로 별도 토큰이 필요 없다. 기본은 비공개이며, 서버가 이미지를 받으려면 `.env.prod`의 `GHCR_READ_TOKEN`이 필요하다. 패키지를 공개로 바꾸면 토큰 없이 받을 수 있다.

## 3. 배포하기

### 자동 배포
main 브랜치에 머지하면 자동으로 진행된다. production 단계에서 승인 요청이 뜬다.

### 수동 배포·롤백
Actions → CD → **Run workflow**

| 입력 | 설명 |
|---|---|
| `environment` | staging 또는 production |
| `image_tag` | 비우면 현재 커밋으로 새로 빌드. **롤백하려면 이전 태그**(예: `sha-1a2b3c4`)를 입력 |

이전 태그는 Actions 기록이나 GHCR 패키지 목록에서 확인할 수 있다.

## 4. 헬스체크와 자동 롤백

`deploy.sh`의 동작 순서다.

```text
1. .deploy/current_tag 에서 현재 태그를 읽어둔다
2. 새 이미지 pull → up -d
3. 컨테이너 안에서 /actuator/health 를 최대 120초 확인
   ├ UP    → current_tag 갱신, 오래된 이미지 정리, 종료 0
   └ 실패  → 로그 출력 → 이전 태그로 되돌림 → 종료 1 (워크플로 실패 표시)
```

헬스체크를 **컨테이너 내부에서** 하는 이유: 외부 도메인으로 확인하면 DNS나 인증서 문제까지 섞여 원인을 가리기 어렵다.

## 5. DB 마이그레이션 주의

Flyway가 앱 기동 시 자동으로 적용한다. 인스턴스가 여러 개여도 Flyway 자체 락이 있어 안전하다.

**중요**: 롤백해도 **DB는 되돌아가지 않는다.** 그래서 스키마 변경은 Expand–Contract 순서로 한다.

```text
1. Expand : 새 컬럼을 nullable로 추가 → 배포 (구버전 코드도 계속 동작)
2. 코드가 새 컬럼을 사용하도록 배포
3. Contract: 제약 추가 / 옛 컬럼 제거 → 배포
```

한 번에 "컬럼 이름 변경" 같은 작업을 하면 롤백 시 구버전 코드가 깨진다.

## 6. 백업

```bash
# cron 등록 (deploy 사용자)
0 3 * * * /opt/app-prod/backup.sh >> /var/log/app-backup.log 2>&1
```

DB 덤프와 업로드 파일을 `$BACKUP_PATH`에 저장하고, 설정한 일수가 지난 파일은 지운다.

복구는 반대로 한다.

```bash
gunzip -c /opt/backups/db-20260917-030000.sql.gz \
  | docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgresql \
    psql -U template -d template
```

> 백업은 **복구를 해봐야** 백업이다. 서버를 만들면 한 번은 복구 연습을 권한다.

## 7. 운영 중 확인

```bash
cd /opt/app-prod

docker compose -f docker-compose.prod.yml --env-file .env.prod --profile postgresql ps
docker compose -f docker-compose.prod.yml --env-file .env.prod --profile postgresql logs -f backend

curl -s https://<도메인>/actuator/health
cat .deploy/current_tag        # 현재 배포된 태그
```

인스턴스를 늘리려면 `.env.prod`의 `BACKEND_REPLICAS`를 올리고 다시 배포한다.

## 8. 문제가 생겼을 때

| 증상 | 확인할 것 |
|---|---|
| 배포가 헬스체크에서 실패 | `logs backend` — 대개 DB 접속 정보나 `JWT_SECRET` 문제 |
| HTTPS 인증서가 안 나옴 | DNS A 레코드, 80/443 포트 개방, Caddy 로그 |
| 이미지를 못 받음 | `GHCR_READ_TOKEN` 만료 여부, 패키지 공개 설정 |
| 로그인은 되는데 새로고침하면 풀림 | `COOKIE_SECURE=true`인데 HTTP로 접속했는지 확인 |
| CORS 오류 | `.env.prod`의 `DOMAIN`과 실제 접속 주소가 같은지 |
