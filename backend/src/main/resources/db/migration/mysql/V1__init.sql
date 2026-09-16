-- 초기 스키마 (MySQL)
-- 주의: postgresql/V1__init.sql 과 반드시 같은 내용을 같은 버전 번호로 유지한다.

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(20)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    last_login_at DATETIME(6),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    deleted_at    DATETIME(6),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_nickname UNIQUE (nickname),
    INDEX idx_users_role_status (role, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE refresh_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    user_agent VARCHAR(255),
    ip_address VARCHAR(45),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_refresh_tokens_user_id (user_id),
    INDEX idx_refresh_tokens_expires_at (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE boards (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug               VARCHAR(30) NOT NULL,
    name               VARCHAR(50) NOT NULL,
    description        VARCHAR(255),
    read_permission    VARCHAR(20) NOT NULL,
    write_permission   VARCHAR(20) NOT NULL,
    comment_enabled    TINYINT(1)  NOT NULL DEFAULT 1,
    attachment_enabled TINYINT(1)  NOT NULL DEFAULT 1,
    active             TINYINT(1)  NOT NULL DEFAULT 1,
    sort_order         INT         NOT NULL DEFAULT 0,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    created_by         BIGINT,
    deleted_at         DATETIME(6),
    CONSTRAINT uk_boards_slug UNIQUE (slug),
    INDEX idx_boards_active_sort (active, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE posts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_id      BIGINT       NOT NULL,
    author_id     BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    content       LONGTEXT     NOT NULL,
    content_text  LONGTEXT     NOT NULL,
    is_notice     TINYINT(1)   NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL,
    view_count    BIGINT       NOT NULL DEFAULT 0,
    comment_count BIGINT       NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    BIGINT,
    deleted_at    DATETIME(6),
    CONSTRAINT fk_posts_board FOREIGN KEY (board_id) REFERENCES boards (id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id),
    INDEX idx_posts_board_list (board_id, deleted_at, status, is_notice, created_at),
    INDEX idx_posts_author (author_id),
    INDEX idx_posts_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE comments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT        NOT NULL,
    author_id  BIGINT        NOT NULL,
    parent_id  BIGINT,
    content    VARCHAR(1000) NOT NULL,
    status     VARCHAR(20)   NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    created_by BIGINT,
    deleted_at DATETIME(6),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id),
    INDEX idx_comments_post (post_id, parent_id, created_at),
    INDEX idx_comments_author (author_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE files (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    storage_key   VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    uploader_id   BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_files_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_files_uploader FOREIGN KEY (uploader_id) REFERENCES users (id),
    INDEX idx_files_status_created (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE post_files (
    post_id    BIGINT      NOT NULL,
    file_id    BIGINT      NOT NULL,
    usage_type VARCHAR(20) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (post_id, file_id),
    CONSTRAINT uk_post_files_file UNIQUE (file_id),
    CONSTRAINT fk_post_files_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_files_file FOREIGN KEY (file_id) REFERENCES files (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
