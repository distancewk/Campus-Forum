package com.campus.ai.service;

import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.exception.ContentRejectedException;
import com.campus.comment.mapper.CommentMapper;
import com.campus.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 异步执行 AI 审核：在发帖/评论主事务提交后触发，调用方无需等待模型延迟。
 * 审核完成后按结果翻转内容状态（LOW→可见，其余→保持待审转人工）。
 * 任何审核失败均保留待审（fail-closed），绝不自动放行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncModerationService {

    private final AiModerationService aiModerationService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @Async("moderationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void moderatePost(Long postId, String title, String content, Long authorId, boolean hasImage) {
        try {
            AiModerationAdvice advice = aiModerationService.review("POST", title, content, authorId, postId);
            // 允许发布需同时满足：风险 LOW、模型未建议 REJECT、且不含图片（图片未审，强制转人工）。
            boolean allow = isAllowed(advice) && !hasImage;
            aiModerationService.bindTargetAndSave(advice, "POST", postId, authorId);
            updatePostStatus(postId, allow ? 1 : 0);
            log.info("AI 审核完成：post={} riskLevel={} action={} hasImage={}",
                    postId, advice == null ? "null" : advice.getRiskLevel(),
                    allow ? "ALLOW" : "REVIEW", hasImage);
        } catch (ContentRejectedException e) {
            // fail-closed：保留待审，等待人工复核。
            log.warn("AI 审核失败（fail-closed），post={} 保留待审：{}", postId, e.getMessage());
            updatePostStatus(postId, 0);
        } catch (RuntimeException e) {
            log.error("异步审核异常，post={} 保留待审", postId, e);
            updatePostStatus(postId, 0);
        }
    }

    /** 是否可自动放行：风险等级为 LOW 且模型未建议 REJECT。 */
    private boolean isAllowed(AiModerationAdvice advice) {
        return advice != null
                && "LOW".equalsIgnoreCase(advice.getRiskLevel())
                && !"REJECT".equalsIgnoreCase(advice.getSuggestedAction());
    }

    @Async("moderationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void moderateComment(Long commentId, Long postId, String content, Long authorId) {
        try {
            AiModerationAdvice advice = aiModerationService.review("COMMENT", null, content, authorId, commentId);
            boolean allow = advice != null && "LOW".equalsIgnoreCase(advice.getRiskLevel());
            aiModerationService.bindTargetAndSave(advice, "COMMENT", commentId, authorId);
            updateCommentStatus(commentId, allow ? 1 : 0);
            // 仅当审核通过时才计入帖子评论数（待审/被拒的评论不计入）。
            if (allow) {
                postMapper.updateCommentCount(postId, 1);
            }
            log.info("AI 审核完成：comment={} riskLevel={} action={}",
                    commentId, advice == null ? "null" : advice.getRiskLevel(), allow ? "ALLOW" : "REVIEW");
        } catch (ContentRejectedException e) {
            log.warn("AI 审核失败（fail-closed），comment={} 保留待审：{}", commentId, e.getMessage());
            updateCommentStatus(commentId, 0);
        } catch (RuntimeException e) {
            log.error("异步审核异常，comment={} 保留待审", commentId, e);
            updateCommentStatus(commentId, 0);
        }
    }

    private void updatePostStatus(Long postId, int status) {
        postMapper.updateStatusById(postId, status);
    }

    private void updateCommentStatus(Long commentId, int status) {
        commentMapper.updateStatusById(commentId, status);
    }
}
