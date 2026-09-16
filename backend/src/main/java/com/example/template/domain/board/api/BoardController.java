package com.example.template.domain.board.api;

import com.example.template.domain.board.api.BoardResponses.BoardDetailResponse;
import com.example.template.domain.board.api.BoardResponses.BoardSummaryResponse;
import com.example.template.domain.board.application.BoardService;
import com.example.template.domain.user.domain.Role;
import com.example.template.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 사용자용 게시판 조회 API. 비로그인도 접근할 수 있다(게시판 권한에 따름). */
@RestController
@RequestMapping("/api/v1/boards")
@Tag(name = "Board", description = "게시판 API")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    @Operation(summary = "게시판 목록", description = "현재 사용자가 읽을 수 있는 활성 게시판만 반환한다.")
    public List<BoardSummaryResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return boardService.getReadableBoards(roleOf(principal)).stream()
                .map(BoardSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "게시판 상세")
    public BoardDetailResponse detail(@PathVariable String slug, @AuthenticationPrincipal AuthPrincipal principal) {
        Role role = roleOf(principal);
        return BoardDetailResponse.from(boardService.getReadableBoard(slug, role), role);
    }

    static Role roleOf(AuthPrincipal principal) {
        return principal == null ? null : Role.valueOf(principal.role());
    }
}
