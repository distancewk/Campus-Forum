package com.campus.admin.controller;

import com.campus.admin.dto.*;
import com.campus.admin.service.AdminService;
import com.campus.common.response.PageQuery;
import com.campus.common.response.PageResult;
import com.campus.common.response.R;
import com.campus.comment.dto.CommentVO;
import com.campus.post.dto.PostListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public R<PageResult<UserAdminVO>> listUsers(@Valid AdminUserQuery query) {
        return R.ok(adminService.listUsers(query));
    }

    /**
     * 禁用/启用用户
     */
    @PutMapping("/users/{id}/status")
    public R<Void> updateUserStatus(@PathVariable Long id,
                                     @RequestBody @Valid StatusRequest request) {
        adminService.updateUserStatus(id, request.getStatus());
        return R.ok();
    }

    /**
     * 获取待审核帖子列表
     */
    @GetMapping("/posts/pending")
    public R<PageResult<PostListVO>> listPendingPosts(@Valid PageQuery query) {
        return R.ok(adminService.listPendingPosts(query));
    }

    /**
     * 审核帖子（通过/拒绝）
     */
    @PutMapping("/posts/{id}/audit")
    public R<Void> auditPost(@PathVariable Long id,
                              @RequestBody @Valid AuditRequest request) {
        adminService.auditPost(id, request.getApproved());
        return R.ok();
    }

    /**
     * 获取待审核评论列表
     */
    @GetMapping("/comments/pending")
    public R<PageResult<CommentVO>> listPendingComments(@Valid PageQuery query) {
        return R.ok(adminService.listPendingComments(query));
    }

    /**
     * 审核评论（通过/拒绝）
     */
    @PutMapping("/comments/{id}/audit")
    public R<Void> auditComment(@PathVariable Long id,
                                 @RequestBody @Valid AuditRequest request) {
        adminService.auditComment(id, request.getApproved());
        return R.ok();
    }

    /**
     * 精华/取消精华评论
     */
    @PutMapping("/comments/{id}/feature")
    public R<Void> toggleCommentFeature(@PathVariable Long id) {
        adminService.toggleCommentFeature(id);
        return R.ok();
    }

    /**
     * 置顶/取消置顶
     */
    @PutMapping("/posts/{id}/pin")
    public R<Void> togglePin(@PathVariable Long id) {
        adminService.togglePin(id);
        return R.ok();
    }

    /**
     * 精华/取消精华
     */
    @PutMapping("/posts/{id}/feature")
    public R<Void> toggleFeature(@PathVariable Long id) {
        adminService.toggleFeature(id);
        return R.ok();
    }

    /**
     * 管理员删除帖子
     */
    @DeleteMapping("/posts/{id}")
    public R<Void> deletePost(@PathVariable Long id) {
        adminService.deletePost(id);
        return R.ok();
    }

    /**
     * 获取数据统计面板
     */
    @GetMapping("/dashboard")
    public R<DashboardVO> getDashboard() {
        return R.ok(adminService.getDashboard());
    }
}
