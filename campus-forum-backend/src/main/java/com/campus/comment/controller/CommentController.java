package com.campus.comment.controller;

import com.campus.comment.dto.CommentCreateRequest;
import com.campus.comment.dto.CommentVO;
import com.campus.comment.service.CommentService;
import com.campus.common.response.PageQuery;
import com.campus.common.response.PageResult;
import com.campus.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 获取评论列表（树形结构，公开接口）
     */
    @GetMapping
    public R<PageResult<CommentVO>> listComments(@PathVariable Long postId,
                                                  @Valid PageQuery query) {
        return R.ok(commentService.listComments(postId, query.getPage(), query.getSize()));
    }

    /**
     * 发表评论（需认证）
     */
    @PostMapping
    public R<CommentVO> createComment(@PathVariable Long postId,
                                       @RequestBody @Valid CommentCreateRequest request) {
        return R.ok(commentService.createComment(postId, request));
    }

    /**
     * 删除评论（需认证，作者或管理员）
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteComment(@PathVariable Long postId, @PathVariable Long id) {
        commentService.deleteComment(postId, id);
        return R.ok();
    }
}
