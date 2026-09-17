# M8. CD(자동 배포)와 운영 문서

> 작성일: 2026-09-17 · 소요 시간: 약 1.5시간 · 관련 요구사항: PRD 16.2~16.8(FR-CICD)

## 1. 이번 단계의 목표

코드를 머지하면 **사람 손 없이 서버까지 반영**되게 만든다. 이 단계가 끝나면 다음이 가능해진다.

- main에 머지 → 이미지 빌드 → GHCR 저장 → staging 자동 배포
- production은 담당자가 승인해야 배포
- 배포 후 헬스체크가 실패하면 **자동으로 이전 버전으로 되돌린다**
- 서버 준비, 백업, 롤백 절차가 문서로 남는다

> 이번 단계는 서버가 없어 실제 배포까지는 확인하지 못했다. 대신 **정적 검사 도구로 문법과 설정을 검증**하고, 사용자가 서버를 준비하면 바로 쓸 수 있게 문서를 정리했다.

## 2. 배경 개념

### 2.1 CI와 CD의 차이
| | CI (지속적 통합) | CD (지속적 배포) |
|---|---|---|
| 목적 | 코드가 **망가지지 않았는지** 확인 | 확인된 코드를 **서버에 올리기** |
| 시점 | PR, push마다 | main 머지, 태그 |
| 결과물 | 테스트 통과 여부 | 실제로 도는 서비스 |

M7이 CI, M8이 CD다.

### 2.2 컨테이너 레지스트리(GHCR)
빌드한 이미지를 보관하는 창고다. 서버는 소스를 받아 빌드하지 않고 **완성된 이미지를 내려받기만** 한다.

```text
GitHub Actions: 빌드 → ghcr.io/사용자/레포-backend:sha-abc1234 로 push
서버:           같은 태그를 pull → 실행
```

이 방식의 장점은 **CI에서 테스트한 그 이미지가 그대로 서버에 간다**는 것이다. 서버에서 다시 빌드하면 "빌드 환경 차이" 때문에 다른 결과가 나올 수 있다.

### 2.3 이미지 태그를 커밋 해시로 붙이는 이유
`latest`만 쓰면 "지금 서버에 뭐가 떠 있는지" 알 수 없고 롤백할 대상도 지목할 수 없다.

```text
sha-1a2b3c4   ← 이 커밋의 이미지. 불변. 롤백 지점으로 쓸 수 있다
latest        ← 편의용 별칭. 계속 바뀐다
v1.2.0        ← 릴리스 태그를 달면 함께 붙는다
```

### 2.4 배포 후 헬스체크와 자동 롤백
배포는 "새 버전을 띄웠다"로 끝나지 않는다. **정말 정상인지 확인**하고, 아니면 되돌려야 한다.

```text
현재 태그 기억 → 새 이미지로 교체 → 헬스체크
                                    ├ 성공: 현재 태그 갱신, 끝
                                    └ 실패: 이전 태그로 재배포 → 워크플로 실패 표시
```

핵심은 **실패를 감추지 않는 것**이다. 롤백에 성공했더라도 종료 코드를 1로 두어 "배포는 실패했다"가 기록에 남게 한다.

헬스체크는 **컨테이너 안에서** 자기 자신을 확인한다. 외부 도메인으로 확인하면 DNS·인증서·방화벽 문제까지 섞여 원인 파악이 어려워진다.

### 2.5 승인 게이트(GitHub Environments)
Environment에 **Required reviewers**를 걸면, 그 job에 도달했을 때 워크플로가 멈추고 승인 버튼이 뜬다.

```yaml
deploy-production:
  environment: production   # 이 이름의 Environment 보호 규칙을 따른다
```

staging은 자동, production은 사람 확인. 자동화와 통제의 균형점이다.

### 2.6 Caddy와 자동 HTTPS
HTTPS를 쓰려면 인증서가 필요하고, Let's Encrypt 인증서는 90일마다 갱신해야 한다. Caddy는 도메인만 적어두면 발급과 갱신을 알아서 한다.

```caddy
{$DOMAIN} {
	reverse_proxy frontend:80
}
```

nginx + certbot 조합보다 설정이 훨씬 짧다. 이 프로젝트에서 nginx는 컨테이너 안에서 정적 파일과 `/api` 프록시를 담당하고, Caddy는 바깥에서 HTTPS만 담당한다.

### 2.7 롤백과 DB 마이그레이션 (중요한 한계)
코드는 되돌릴 수 있지만 **DB는 되돌아가지 않는다.**

```text
v2 배포: 컬럼 이름 변경 (name → full_name)
v1 롤백: v1 코드는 name 컬럼을 찾는다 → 깨진다 ❌
```

그래서 스키마 변경은 **Expand–Contract** 순서로 한다.

```text
1. Expand   : full_name 컬럼 추가(nullable) → 배포. v1 코드도 계속 동작한다
2. 전환      : 새 컬럼을 쓰는 코드 배포
3. Contract : name 컬럼 제거 → 배포
```

각 단계 사이에 롤백해도 안전하다. 한 번에 하면 롤백 경로가 막힌다.

## 3. 따라하기

### 3.1 CD 워크플로 구조

```yaml
on:
  push:
    branches: [main]          # 머지되면 자동
    tags: ['v*']              # 릴리스 태그
  workflow_dispatch:          # 수동 실행(롤백용 입력 포함)
    inputs:
      environment: [staging, production]
      image_tag: '롤백할 태그 (비우면 새로 빌드)'

jobs:
  build-and-push:     # image_tag 가 비었을 때만 실행
  deploy-staging:     # environment: staging
  deploy-production:  # environment: production (승인 대기)
```

롤백은 "빌드를 건너뛰고 기존 태그로 배포"다. 새로 빌드하지 않으므로 빠르고, **이미 검증된 이미지**를 그대로 쓴다.

### 3.2 중복 제거: 복합 액션
staging과 production의 배포 절차가 똑같다. 복사해 두면 한쪽만 고치는 실수가 생긴다. 공통 부분을 `.github/actions/deploy`로 뺐다.

```yaml
- name: 배포 실행
  uses: ./.github/actions/deploy
  with:
    image_tag: ${{ ... }}
    ssh_host: ${{ secrets.SSH_HOST }}
    deploy_path: ${{ vars.DEPLOY_PATH }}
```

### 3.3 SSH 접속 시 호스트 검증

```bash
echo "${{ inputs.ssh_known_hosts }}" > ~/.ssh/known_hosts
```

`StrictHostKeyChecking=no`로 검증을 꺼버리는 예제가 많지만, 그러면 중간자 공격을 막지 못한다. `ssh-keyscan` 결과를 Secret에 넣어두고 검증한다.

키 파일은 작업이 끝나면 지운다.

```yaml
- name: SSH 키 정리
  if: always()
  run: rm -f ~/.ssh/id_deploy
```

### 3.4 배포 스크립트 핵심

```bash
PREVIOUS_TAG="$(cat "$CURRENT_TAG_FILE")"      # 롤백 대상 기억

deploy_tag "$NEW_TAG"                           # pull + up -d

if health_check; then
    echo "$NEW_TAG" > "$CURRENT_TAG_FILE"       # 성공: 현재 태그 갱신
    exit 0
fi

"${COMPOSE[@]}" logs --tail=100 backend         # 실패 원인을 로그에 남긴다
deploy_tag "$PREVIOUS_TAG"                      # 롤백
exit 1                                          # 워크플로는 실패로 표시
```

### 3.5 운영 compose에서 달라지는 것

| 항목 | 개발 | 운영 |
|---|---|---|
| 이미지 | 로컬 빌드 | GHCR에서 pull |
| DB 포트 | 호스트에 노출 | **노출하지 않음** |
| 쿠키 | `Secure=false` | `Secure=true` (HTTPS 전용) |
| Swagger | 켬 | **끔** |
| 앞단 | 없음 | Caddy(HTTPS) |

## 4. 결정과 이유

| 결정 | 고려한 대안 | 선택 이유 |
|---|---|---|
| 이미지 태그를 커밋 해시로 | latest만 사용 | 무엇이 떠 있는지 알 수 있고 롤백 지점이 생긴다. |
| 롤백 시 재빌드 안 함 | 해당 커밋으로 다시 빌드 | 이미 검증된 이미지를 쓰는 게 빠르고 안전하다. |
| 헬스체크는 컨테이너 내부에서 | 외부 도메인으로 | DNS·인증서 문제가 섞이지 않는다. |
| 롤백 성공해도 종료 코드 1 | 롤백했으니 성공 처리 | 배포 실패 사실이 기록에 남아야 한다. |
| 배포 로직을 복합 액션으로 | 워크플로에 두 번 작성 | 한쪽만 고치는 실수를 막는다. |
| Caddy로 HTTPS | nginx + certbot | 인증서 발급·갱신이 자동이라 운영 부담이 적다. |
| `.env.prod`는 서버에만 | 워크플로에서 생성 | 비밀값을 CI 로그나 레포에 남기지 않는다. |
| known_hosts 검증 | StrictHostKeyChecking=no | 중간자 공격 방어. 예제 코드를 그대로 쓰면 안 되는 부분이다. |

## 5. 겪은 문제와 해결

서버가 없어 실제 배포는 못 했지만, **정적 검사 도구로 문법과 설정을 검증**하면서 여러 문제를 잡았다.

### 문제 1: 스크립트가 Windows 줄바꿈으로 저장됐다
```text
SC1017 (error): Literal carriage return. Run script through tr -d '\r'
```
- **원인**: Windows에서 파일을 쓰면서 줄바꿈이 CRLF가 됐다. 리눅스 서버에서 실행하면 `bash^M: bad interpreter` 같은 오류가 난다.
- **해결**: 셸 스크립트를 LF로 저장했다. `.gitattributes`에 `*.sh text eol=lf`가 이미 있어 저장소에는 LF로 들어가지만, **작업 폴더의 파일도 맞춰둬야** 검사 도구가 제대로 돈다.
- **배운 점**: Windows에서 리눅스용 스크립트를 만들 때 늘 확인해야 하는 부분이다. shellcheck가 없었으면 서버에서야 알았을 것이다.

### 문제 2: shellcheck 지시어가 먹지 않았다
```bash
# shellcheck disable=SC1090  # 런타임에만 존재하는 파일이라 검사할 수 없다
set -a; . "./$ENV_FILE"; set +a
```
- **원인**: 지시어 뒤에 설명을 덧붙이면 인식되지 않는다. 또 한 줄에 여러 명령을 붙이면 적용 범위가 모호해진다.
- **해결**: 설명은 윗줄에 따로 쓰고, 지시어는 단독으로, 명령도 한 줄씩 분리했다.

```bash
# .env.prod 는 서버에만 존재하므로 정적 검사 대상이 아니다.
set -a
# shellcheck source=/dev/null
. "./$ENV_FILE"
set +a
```

### 문제 3: actionlint가 CI에서 미사용 변수를 지적
```text
SC2034:warning: i appears unused. Verify use (or export if used externally)
```
- **원인**: `for i in $(seq 1 60)` 에서 `i`를 쓰지 않았다.
- **해결**: `for _ in $(seq 1 60)` 으로 바꿨다. 사소하지만, 이런 경고가 쌓이면 진짜 문제를 못 본다.
- **덤**: actionlint는 워크플로 안의 셸 스크립트까지 검사해준다. M7에서 쓴 CI 파일의 문제를 M8에서 발견했다.

### 문제 4 (예방): 운영 설정이 의도대로 해석되는지
compose 파일은 변수 치환이 많아 눈으로 보면 틀리기 쉽다. 가짜 `.env.prod`로 실제 해석 결과를 확인했다.

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod.test --profile postgresql config
```

```text
COOKIE_SECURE: "true"                      ← HTTPS 전용 쿠키
CORS_ALLOWED_ORIGINS: https://example.com  ← 도메인이 제대로 들어감
SWAGGER_ENABLED: "false"                   ← 운영에서 API 문서 비공개
image: ghcr.io/test/test-backend:latest    ← 이미지 주소 조합 정상
```

두 DB 프로필 모두 확인했다.

## 6. 확인 방법

### 정적 검사 (지금 확인한 것)

```bash
# 셸 스크립트
docker run --rm -v "$PWD/deploy:/scripts" -w /scripts koalaman/shellcheck:stable \
  deploy.sh backup.sh server-setup.sh
# → 경고 0

# GitHub Actions 워크플로
docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest -color
# → 문제 없음

# 운영 compose 설정
cd deploy && docker compose -f docker-compose.prod.yml --env-file .env.prod.test --profile postgresql config
# → OK (mysql 프로필도 OK)
```

### 실제 배포 확인 (🙋 서버 준비 후 사용자 확인 필요)

[docs/deployment.md](../deployment.md)의 순서대로 진행한 뒤:

1. main에 머지 → Actions에서 이미지 빌드 → GHCR에 `sha-*` 태그 확인
2. staging 자동 배포 → `https://staging.도메인/actuator/health` 가 `UP`
3. production은 승인 버튼을 눌러야 진행
4. **롤백 확인**: `.env.prod`의 `JWT_SECRET`을 일부러 짧게 바꾸고 배포 → 헬스체크 실패 → 이전 태그로 자동 복구되는지
5. Actions → CD → Run workflow에 이전 태그를 넣어 수동 롤백
6. `./backup.sh` 실행 → 백업 파일 생성 확인

## 7. 회고와 다음 단계

**잘된 점**
- 서버가 없어도 **검사할 수 있는 것은 전부 검사**했다(shellcheck, actionlint, compose config). "일단 작성만 했다"로 끝내지 않았다.
- 롤백을 "되돌리면 성공"이 아니라 "되돌렸지만 배포는 실패"로 기록하게 했다. 실패가 묻히지 않는다.
- DB 롤백 불가라는 한계를 문서에 명시하고 Expand–Contract 절차를 남겼다. 나중에 사고가 나기 쉬운 지점이다.

**아쉬운 점 / 개선할 점**
- 실제 서버에서 한 번도 돌려보지 못했다. 특히 자동 롤백은 **일부러 실패시켜 봐야** 믿을 수 있다.
- 무중단 배포가 아니다. 컨테이너가 교체되는 동안 수 초 끊긴다. 필요해지면 blue-green이나 rolling update를 검토한다.
- 모니터링과 알림이 없다. 배포 실패나 서버 이상을 Actions 기록으로만 알 수 있다.

**프로젝트 전체를 마치며**
M0부터 M8까지, 빈 폴더에서 시작해 인증·게시판·관리자 기능을 갖춘 템플릿과 그 배포 경로까지 만들었다. 되돌아보면 **실제로 돌려봤을 때만 드러난 문제**가 가장 많았다.

- M2: 예외를 던지면 방금 한 저장이 롤백된다 (재사용 탐지)
- M4: MySQL에서만 컬럼 타입이 안 맞는다
- M7: 컨테이너에서만 파일 업로드가 막힌다 (볼륨 권한)
- M7: 브라우저에서만 로그인이 403이다 (프록시 뒤 CORS)

테스트를 두 DB로 돌리고, 배포 형태로 띄워보고, 실제 브라우저로 확인한 것이 모두 값을 했다.
