package com.example.template.domain.board.api;

import com.example.template.domain.board.api.BoardResponses.AdminBoardResponse;
import com.example.template.domain.board.application.BoardCommand;
import com.example.template.domain.board.application.BoardService;
import com.example.template.domain.board.domain.Board;
import com.example.template.domain.board.domain.Permission;
import com.example.template.domain.post.application.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자용 게시판 관리 API. SecurityConfig에서 ADMIN만 접근하도록 막혀 있다. */
@RestController
@RequestMapping("/api/v1/admin/boards")
@Tag(name = "Admin Board", description = "게시판 관리 API (관리자)")
public class AdminBoardController {

    private final BoardService boardService;
    private final PostService postService;

    public AdminBoardController(BoardService boardService, PostService postService) {
        this.boardService = boardService;
        this.postService = postService;
    }

    public record BoardUpsertRequest(
            @NotBlank
            @Size(min = 2, max = 30)
            @Pattern(regexp = "^[a-z0-9-]+$", message = "주소는 영문 소문자, 숫자, 하이픈만 사용할 수 있습니다.")
            String slug,

            @NotBlank @Size(min = 1, max = 50) String name,
            @Size(max = 255) String description,
            @NotNull Permission readPermission,
            @NotNull Permission writePermission,
            boolean commentEnabled,
            boolean attachmentEnabled,
            boolean active) {

        BoardCommand toCommand() {
            return new BoardCommand(
                    slug,
                    name,
                    description,
                    readPermission,
                    writePermission,
                    commentEnabled,
                    attachmentEnabled,
                    active);
        }
    }

    public record ReorderRequest(@NotNull List<Long> boardIds) {}

    @GetMapping
    @Operation(summary = "게시판 목록(관리자)", description = "비활성 게시판과 게시글 수를 포함한다.")
    public List<AdminBoardResponse> list() {
        return boardService.getAdminBoards().stream()
                .map(AdminBoardResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "게시판 생성")
    public ResponseEntity<AdminBoardResponse> create(@Valid @RequestBody BoardUpsertRequest request) {
        Board board = boardService.create(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminBoardResponse.from(board, 0));
    }

    @PutMapping("/{id}")
    @Operation(summary = "게시판 수정")
    public AdminBoardResponse update(@PathVariable Long id, @Valid @RequestBody BoardUpsertRequest request) {
        Board board = boardService.update(id, request.toCommand());
        return AdminBoardResponse.from(board, postService.countByBoard(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "게시판 삭제", description = "게시글이 있으면 409를 반환한다. 이 경우 비활성화를 사용한다.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boardService.delete(id, postService.countByBoard(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/order")
    @Operation(summary = "게시판 정렬 순서 변경")
    public ResponseEntity<Void> reorder(@Valid @RequestBody ReorderRequest request) {
        boardService.reorder(request.boardIds());
        return ResponseEntity.noContent().build();
    }
}
