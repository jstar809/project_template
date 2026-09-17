# syntax=docker/dockerfile:1

# ── 1단계: 빌드 ───────────────────────────────────────────────
FROM node:26-alpine AS builder
WORKDIR /app

# package.json과 lock 파일만 먼저 복사 → 의존성 레이어 캐시 활용
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Vite는 빌드 시점에 환경변수를 코드에 박아 넣는다(런타임 주입 불가).
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# ── 2단계: 실행 ───────────────────────────────────────────────
# 빌드 결과는 정적 파일이므로 nginx가 그대로 서빙한다.
FROM nginx:alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q --spider http://localhost/ || exit 1
