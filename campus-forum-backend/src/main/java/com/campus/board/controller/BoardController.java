package com.campus.board.controller;

import com.campus.board.dto.BoardCreateRequest;
import com.campus.board.dto.BoardUpdateRequest;
import com.campus.board.dto.BoardVO;
import com.campus.board.service.BoardService;
import com.campus.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    /**
     * 获取板块列表（公开接口）
     */
    @GetMapping("/boards")
    public R<List<BoardVO>> listBoards() {
        return R.ok(boardService.listBoards());
    }

    /**
     * 创建板块（管理员）
     */
    @PostMapping("/admin/boards")
    @PreAuthorize("hasRole('ADMIN')")
    public R<BoardVO> createBoard(@RequestBody @Valid BoardCreateRequest request) {
        return R.ok(boardService.createBoard(request));
    }

    /**
     * 更新板块（管理员）
     */
    @PutMapping("/admin/boards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> updateBoard(@PathVariable Long id,
                               @RequestBody @Valid BoardUpdateRequest request) {
        boardService.updateBoard(id, request);
        return R.ok();
    }

    /**
     * 删除板块（管理员，需检查是否有帖子）
     */
    @DeleteMapping("/admin/boards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> deleteBoard(@PathVariable Long id) {
        boardService.deleteBoard(id);
        return R.ok();
    }
}
