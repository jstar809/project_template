package com.example.template.domain.board.application;

import com.example.template.domain.board.domain.Board;
import com.example.template.domain.board.domain.BoardErrorCode;
import com.example.template.domain.board.domain.BoardRepository;
import com.example.template.domain.board.domain.Permission;
import com.example.template.domain.board.infra.mybatis.BoardQueryMapper;
import com.example.template.domain.user.domain.Role;
import com.example.template.global.config.CacheConfig;
import com.example.template.global.error.BusinessException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 업무 로직.
 *
 * <p>쓰기는 JPA, 목록 조회는 MyBatis를 쓴다(PRD 8.2).
 */
@Service
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardQueryMapper boardQueryMapper;
    private final Clock clock;

    public BoardService(BoardRepository boardRepository, BoardQueryMapper boardQueryMapper, Clock clock) {
        this.boardRepository = boardRepository;
        this.boardQueryMapper = boardQueryMapper;
        this.clock = clock;
    }

    /** 현재 사용자가 읽을 수 있는 게시판 목록. */
    public List<BoardSummary> getReadableBoards(Role role) {
        return boardQueryMapper.selectReadableBoards(allowedPermissions(role));
    }

    /** 읽기 권한을 확인한 뒤 게시판을 돌려준다. 없으면 404, 권한이 없으면 403. */
    public Board getReadableBoard(String slug, Role role) {
        Board board = boardRepository
                .findBySlug(slug)
                .orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_NOT_FOUND));
        if (!board.canRead(role)) {
            throw new BusinessException(BoardErrorCode.BOARD_READ_FORBIDDEN);
        }
        return board;
    }

    /** 쓰기 권한까지 확인한다. */
    public Board getWritableBoard(String slug, Role role) {
        Board board = getReadableBoard(slug, role);
        if (!board.canWrite(role)) {
            throw new BusinessException(BoardErrorCode.BOARD_WRITE_FORBIDDEN);
        }
        return board;
    }

    public Board getById(Long id) {
        return boardRepository.findById(id).orElseThrow(() -> new BusinessException(BoardErrorCode.BOARD_NOT_FOUND));
    }

    // ---- 관리자 기능 ----

    public List<AdminBoardSummary> getAdminBoards() {
        return boardQueryMapper.selectAdminBoards();
    }

    @Transactional
    @CacheEvict(value = CacheConfig.BOARDS, allEntries = true)
    public Board create(BoardCommand command) {
        if (boardRepository.existsBySlug(command.slug())) {
            throw new BusinessException(BoardErrorCode.BOARD_SLUG_DUPLICATED);
        }
        int nextOrder = boardRepository.findAllActiveOrNot().size() + 1;
        Board board = Board.create(
                command.slug(),
                command.name(),
                command.description(),
                command.readPermission(),
                command.writePermission(),
                command.commentEnabled(),
                command.attachmentEnabled(),
                command.active(),
                nextOrder);
        return boardRepository.save(board);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.BOARDS, allEntries = true)
    public Board update(Long id, BoardCommand command) {
        Board board = getById(id);
        String newSlug = Board.normalizeSlug(command.slug());
        if (!board.getSlug().equals(newSlug) && boardRepository.existsBySlug(newSlug)) {
            throw new BusinessException(BoardErrorCode.BOARD_SLUG_DUPLICATED);
        }
        board.update(
                command.slug(),
                command.name(),
                command.description(),
                command.readPermission(),
                command.writePermission(),
                command.commentEnabled(),
                command.attachmentEnabled(),
                command.active());
        return board;
    }

    /**
     * 게시판 삭제(soft delete).
     *
     * <p>게시글이 하나라도 있으면 삭제하지 않고 409를 돌려준다. 실수로 글 전체를 숨기는 사고를 막기 위해서다.
     */
    @Transactional
    @CacheEvict(value = CacheConfig.BOARDS, allEntries = true)
    public void delete(Long id, long postCount) {
        if (postCount > 0) {
            throw new BusinessException(BoardErrorCode.BOARD_HAS_POSTS);
        }
        getById(id).softDelete(clock.instant());
    }

    /** 전달된 순서대로 sortOrder를 1부터 다시 매긴다. */
    @Transactional
    @CacheEvict(value = CacheConfig.BOARDS, allEntries = true)
    public void reorder(List<Long> boardIds) {
        List<Board> boards = boardRepository.findAllByIdIn(boardIds);
        for (int i = 0; i < boardIds.size(); i++) {
            Long boardId = boardIds.get(i);
            int order = i + 1;
            boards.stream()
                    .filter(board -> board.getId().equals(boardId))
                    .findFirst()
                    .ifPresent(board -> board.changeSortOrder(order));
        }
    }

    /** 역할이 만족하는 권한 값들. MyBatis IN 절에 넘긴다. */
    private static List<String> allowedPermissions(Role role) {
        List<String> permissions = new ArrayList<>();
        for (Permission permission : Permission.values()) {
            if (permission.allows(role)) {
                permissions.add(permission.name());
            }
        }
        return permissions;
    }
}
