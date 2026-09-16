-- 기본 게시판 시드 (MySQL)
-- 관리자 계정은 SQL이 아니라 기동 시 초기화 로직(M2)에서 생성한다. 비밀번호 해시가 필요하기 때문이다.

INSERT INTO boards (slug, name, description, read_permission, write_permission,
                    comment_enabled, attachment_enabled, active, sort_order, created_at, updated_at)
VALUES ('notice', '공지사항', '운영 공지를 올리는 게시판입니다.', 'ANONYMOUS', 'ADMIN',
        0, 1, 1, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       ('free', '자유게시판', '자유롭게 글을 쓰는 게시판입니다.', 'ANONYMOUS', 'USER',
        1, 1, 1, 2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
