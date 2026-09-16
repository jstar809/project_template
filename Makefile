DB ?= postgresql

.PHONY: help db-up db-down be-run be-test be-test-all fe-install fe-run fe-check test lint clean

help:
	@echo "make db-up DB=postgresql|mysql   DB 컨테이너 실행"
	@echo "make db-down                     DB 컨테이너 종료"
	@echo "make be-run                      백엔드 실행 (http://localhost:8080)"
	@echo "make be-test DB=postgresql       백엔드 테스트 (선택한 DB)"
	@echo "make be-test-all                 백엔드 테스트 (PostgreSQL + MySQL)"
	@echo "make fe-run                      프론트 개발 서버 (http://localhost:5173)"
	@echo "make fe-check                    프론트 lint + typecheck + build"
	@echo "make test                        백엔드(기본 DB) + 프론트 검사"
	@echo "make lint                        spotlessApply + eslint --fix"

db-up:
	docker compose --profile $(DB) up -d

db-down:
	docker compose --profile postgresql --profile mysql down

be-run:
	cd backend && ./gradlew bootRun

be-test:
	cd backend && ./gradlew test -PdbVendor=$(DB)

be-test-all:
	cd backend && ./gradlew test -PdbVendor=postgresql
	cd backend && ./gradlew test -PdbVendor=mysql

fe-install:
	cd frontend && npm install

fe-run:
	cd frontend && npm run dev

fe-check:
	cd frontend && npm run lint && npm run typecheck && npm run build

test: be-test fe-check

lint:
	cd backend && ./gradlew spotlessApply
	cd frontend && npm run lint:fix && npm run format

clean:
	cd backend && ./gradlew clean
	rm -rf frontend/dist frontend/node_modules/.vite
