package com.campus.admin.service;

import com.campus.admin.dto.*;
import com.campus.ai.service.AiModerationService;
import com.campus.ai.service.KnowledgeIngestionService;
import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.comment.dto.CommentVO;
import com.campus.comment.entity.Comment;
import com.campus.comment.mapper.CommentMapper;
import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import com.campus.common.response.PageQuery;
import com.campus.common.response.PageResult;
import com.campus.common.util.SecurityUtil;
import com.campus.post.dto.PostListVO;
import com.campus.post.entity.Post;
import com.campus.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final AiModerationService aiModerationService;
    private final KnowledgeIngestionService knowledgeIngestionService;

    /**
     * 分页查询用户列表
     */
    public PageResult<UserAdminVO> listUsers(AdminUserQuery query) {
        int offset = query.getOffset();
        int size = query.getSize();

        // 查询用户列表
        List<UserAdminVO> records = userMapper.selectAdminUserList(
                query.getKeyword(), query.getStatus(), offset, size);

        // 查询总数
        long total = userMapper.countAdminUserList(query.getKeyword(), query.getStatus());

        return new PageResult<>(records, total, query.getPage(), query.getSize());
    }

    /**
     * 禁用/启用用户
     */
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        Long currentUserId = SecurityUtil.requireCurrentUserId();

        // 不能操作自己
        if (currentUserId.equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "不能禁用自己的账号");
        }

        // 查询目标用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 不能操作其他管理员
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "不能操作其他管理员账号");
        }

        // 更新状态
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 查询待审核帖子
     */
    public PageResult<PostListVO> listPendingPosts(PageQuery query) {
        int offset = query.getOffset();
        int size = query.getSize();

        List<PostListVO> records = postMapper.selectPendingPosts(offset, size);
        long total = postMapper.countPendingPosts();

        return new PageResult<>(records, total, query.getPage(), query.getSize());
    }

    /**
     * 审核帖子
     */
    @Transactional
    public void auditPost(Long postId, boolean approved) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Long adminId = SecurityUtil.requireCurrentUserId();
        if (approved) {
            // 通过审核
            post.setStatus(1);
            postMapper.updateById(post);
            aiModerationService.markAdminReviewed("POST", postId, "ADMIN_APPROVED", adminId);
            runAfterCommit(() -> safeIndexPublishedPost(postId));
        } else {
            // 拒绝 - 逻辑删除
            post.setStatus(-1);
            post.setDeleted(1);
            postMapper.updateById(post);
            aiModerationService.markAdminReviewed("POST", postId, "ADMIN_REJECTED", adminId);
        }
    }

    /**
     * 查询待审核评论
     */
    public PageResult<CommentVO> listPendingComments(PageQuery query) {
        int offset = query.getOffset();
        int size = query.getSize();

        List<CommentVO> records = commentMapper.selectPendingComments(offset, size);
        long total = commentMapper.countPendingComments();

        return new PageResult<>(records, total, query.getPage(), query.getSize());
    }

    /**
     * 审核评论
     */
    @Transactional
    public void auditComment(Long commentId, boolean approved) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }

        Long adminId = SecurityUtil.requireCurrentUserId();
        boolean wasPublished = Integer.valueOf(1).equals(comment.getStatus())
                && !Integer.valueOf(1).equals(comment.getDeleted());
        if (approved) {
            comment.setStatus(1);
            comment.setDeleted(0);
            commentMapper.updateById(comment);
            if (!wasPublished) {
                postMapper.updateCommentCount(comment.getPostId(), 1);
            }
            aiModerationService.markAdminReviewed("COMMENT", commentId, "ADMIN_APPROVED", adminId);
        } else {
            comment.setStatus(-1);
            comment.setDeleted(1);
            commentMapper.updateById(comment);
            if (wasPublished) {
                postMapper.updateCommentCount(comment.getPostId(), -1);
            }
            aiModerationService.markAdminReviewed("COMMENT", commentId, "ADMIN_REJECTED", adminId);
        }
    }

    /**
     * 切换评论精华状态
     */
    @Transactional
    public void toggleCommentFeature(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (!Integer.valueOf(1).equals(comment.getStatus()) || Integer.valueOf(1).equals(comment.getDeleted())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅可设置已发布评论为精华");
        }

        boolean enabled = !Boolean.TRUE.equals(comment.getIsFeatured());
        comment.setIsFeatured(enabled);
        commentMapper.updateById(comment);
        if (enabled) {
            runAfterCommit(() -> safeIndexFeaturedComment(commentId));
        }
    }

    /**
     * 切换置顶状态
     */
    @Transactional
    public void togglePin(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        post.setIsPinned(!post.getIsPinned());
        postMapper.updateById(post);
    }

    /**
     * 切换精华状态
     */
    @Transactional
    public void toggleFeature(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        post.setIsFeatured(!post.getIsFeatured());
        postMapper.updateById(post);
    }

    /**
     * 管理员强制删除帖子
     */
    @Transactional
    public void deletePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        post.setDeleted(1);
        postMapper.updateById(post);
    }

    private void safeIndexPublishedPost(Long postId) {
        try {
            knowledgeIngestionService.indexPublishedPost(postId);
        } catch (RuntimeException e) {
            log.warn("Index published post failed, postId={}", postId, e);
        }
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void safeIndexFeaturedComment(Long commentId) {
        try {
            knowledgeIngestionService.indexFeaturedComment(commentId);
        } catch (RuntimeException e) {
            log.warn("Index featured comment failed, commentId={}", commentId, e);
        }
    }

    /**
     * 获取数据统计面板
     */
    public DashboardVO getDashboard() {
        DashboardVO dashboard = new DashboardVO();

        // 总用户数
        dashboard.setTotalUsers(userMapper.selectTotalUsers());

        // 总帖子数
        dashboard.setTotalPosts(postMapper.selectTotalPosts());

        // 总评论数
        dashboard.setTotalComments(commentMapper.selectTotalComments());

        // 今日新增用户
        dashboard.setTodayNewUsers(userMapper.selectTodayNewUsers());

        // 今日新增帖子
        dashboard.setTodayNewPosts(postMapper.selectTodayNewPosts());

        // 7日活跃用户数
        dashboard.setActiveUsers(userMapper.selectActiveUsers(7));

        // 各板块帖子数统计
        dashboard.setBoardStats(postMapper.selectBoardStats());

        return dashboard;
    }
}
