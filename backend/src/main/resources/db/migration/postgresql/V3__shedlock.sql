-- ShedLock: 여러 인스턴스가 떠 있어도 스케줄러가 한 번만 실행되게 하는 분산 락 테이블 (PostgreSQL)

CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
