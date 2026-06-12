package com.campus.comment.service;

import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.service.AiModerationService;
import com.campus.comment.dto.CommentCreateRequest;
import com.campus.comment.dto.CommentVO;
import com.campus.comment.entity.Comment;
import com.campus.comment.mapper.CommentMapper;
import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import com.campus.common.response.PageResult;
import com.campus.common.util.SecurityUtil;
import com.campus.interaction.mapper.LikeMapper;
import com.campus.post.entity.Post;
import com.campus.post.mapper.PostMapper;
import com.campus.user.dto.AuthorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final LikeMapper likeMapper;
    private final AiModerationService aiModerationService;

    /**
     * 获取评论列表（树形结构）
     */
    public PageResult<CommentVO> listComments(Long postId, int page, int size) {
        // 1. 查询顶层评论总数
        long total = commentMapper.countTopLevelComments(postId);

        // 2. 分页查询顶层评论
        int offset = (page - 1) * size;
        List<CommentVO> topLevel = commentMapper.selectTopLevelComments(postId, offset, size);

        if (topLevel.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), total, page, size);
        }

        // 3. 批量查询子回复（避免 N+1）
        List<Long> parentIds = topLevel.stream()
                .map(CommentVO::getId)
                .collect(Collectors.toList());
        List<CommentVO> replies = commentMapper.selectRepliesByParentIds(parentIds);

        // 4. 按 parentId 分组子回复
        Map<Long, List<CommentVO>> repliesMap = replies.stream()
                .collect(Collectors.groupingBy(CommentVO::getParentId));

        // 5. 查询当前用户点赞状态
        Long currentUserId = SecurityUtil.getCurrentUserId();
        Set<Long> likedCommentIds = Collections.emptySet();
        if (currentUserId != null) {
            // 收集所有评论 ID（顶层 + 回复）
            List<Long> allCommentIds = new ArrayList<>();
            for (CommentVO vo : topLevel) {
                allCommentIds.add(vo.getId());
                List<CommentVO> childReplies = repliesMap.getOrDefault(vo.getId(), Collections.emptyList());
                for (CommentVO reply : childReplies) {
                    allCommentIds.add(reply.getId());
                }
            }
            if (!allCommentIds.isEmpty()) {
                likedCommentIds = likeMapper.selectLikedTargetIds(currentUserId, "COMMENT", allCommentIds);
            }
        }

        // 6. 组装树形结构
        for (CommentVO vo : topLevel) {
            List<CommentVO> childReplies = repliesMap.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setReplies(childReplies);

            // 设置 isLiked / isOwner
            vo.setIsLiked(likedCommentIds.contains(vo.getId()));
            vo.setIsOwner(currentUserId != null && currentUserId.equals(vo.getAuthor().getId()));
            vo.setPendingReview(false);

            // 为子回复设置 isLiked / isOwner
            for (CommentVO reply : childReplies) {
                reply.setIsLiked(likedCommentIds.contains(reply.getId()));
                reply.setIsOwner(currentUserId != null && currentUserId.equals(reply.getAuthor().getId()));
                reply.setPendingReview(false);
            }
        }

        return new PageResult<>(topLevel, total, page, size);
    }

    /**
     * 发表评论
     */
    @Transactional
    public CommentVO createComment(Long postId, CommentCreateRequest request) {
        // 1. 校验帖子存在且未删除
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        // 2. 如果是楼中楼回复，校验父评论存在且属于该帖子
        if (request.getParentId() != null) {
            Comment parent = commentMapper.selectById(request.getParentId());
            if (parent == null || !parent.getPostId().equals(postId)) {
                throw new BusinessException(ResultCode.NOT_FOUND, "父评论不存在");
            }
            // 只支持一级回复，不允许嵌套更深
            if (parent.getParentId() != null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "只支持一级回复");
            }
        }

        // 3. XSS 过滤（纯文本，escape HTML）
        String safeContent = escapeHtml(request.getContent().trim());

        Long currentUserId = SecurityUtil.requireCurrentUserId();

        // 4. 插入评论
        Comment comment = new Comment();
        comment.setContent(safeContent);
        comment.setAuthorId(currentUserId);
        comment.setPostId(postId);
        comment.setParentId(request.getParentId());
        comment.setReplyToUserId(request.getReplyToUserId());
        comment.setLikeCount(0);
        AiModerationAdvice advice = aiModerationService.review("COMMENT", null, safeContent, currentUserId, null);
        boolean pendingReview = advice == null || !"LOW".equalsIgnoreCase(advice.getRiskLevel());
        comment.setStatus(pendingReview ? 0 : 1);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setDeleted(0);
        commentMapper.insert(comment);
        aiModerationService.bindTargetAndSave(advice, "COMMENT", comment.getId(), currentUserId);

        // 5. 帖子评论数 +1
        if (!pendingReview) {
            postMapper.updateCommentCount(postId, 1);
        }

        // 6. 返回 CommentVO
        CommentVO vo = buildCommentVO(comment, currentUserId);
        vo.setPendingReview(pendingReview);
        return vo;
    }

    /**
     * 删除评论（作者或管理员）
     */
    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        // 1. 校验评论存在
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getPostId().equals(postId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }

        // 2. 校验权限（作者或管理员）
        Long currentUserId = SecurityUtil.requireCurrentUserId();
        if (!currentUserId.equals(comment.getAuthorId()) && !SecurityUtil.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此评论");
        }

        // 3. 逻辑删除
        comment.setDeleted(1);
        commentMapper.updateById(comment);

        // 4. 帖子评论数 -1
        postMapper.updateCommentCount(postId, -1);
    }

    /**
     * 构建单个 CommentVO（用于创建后返回）
     */
    private CommentVO buildCommentVO(Comment comment, Long currentUserId) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setLikeCount(comment.getLikeCount());
        vo.setStatus(comment.getStatus());
        vo.setPendingReview(Integer.valueOf(0).equals(comment.getStatus()));
        vo.setIsFeatured(comment.getIsFeatured());
        vo.setIsLiked(false);
        vo.setIsOwner(true);
        vo.setCreatedAt(comment.getCreatedAt());

        // 作者信息（简化：从 SecurityContext 获取的当前用户即作者）
        // 为了获取准确的 nickname/avatar，此处不做额外查询，前端可根据 userId 请求
        AuthorVO author = new AuthorVO();
        author.setId(currentUserId);
        vo.setAuthor(author);

        // replyToUser
        if (comment.getReplyToUserId() != null) {
            AuthorVO replyToUser = new AuthorVO();
            replyToUser.setId(comment.getReplyToUserId());
            vo.setReplyToUser(replyToUser);
        }

        vo.setReplies(Collections.emptyList());
        return vo;
    }

    /**
     * 转义 HTML 特殊字符，防止 XSS
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
