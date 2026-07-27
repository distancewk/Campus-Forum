package com.campus.ai.service;

import com.campus.ai.config.AiProperties;
import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.exception.ContentRejectedException;
import com.campus.comment.mapper.CommentMapper;
import com.campus.notification.service.NotificationService;
import com.campus.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 异步执行 AI 审核：在发帖/评论主事务提交后触发，调用方无需等待模型延迟。
 *
 * <p>发布策略（第一性原理：核心动作不依赖可选/付费/会宕机的第三方服务）：
 * <ul>
 *   <li>AI 未启用（campus.ai.enabled=false 或 moderation-enabled=false）→ 永不调用模型，保持现有状态（默认已发布）。</li>
 *   <li>mode=post（默认，post-hoc）：内容先公开，AI 事后复核；明确 REJECT 才下架，审核服务抖动不下架。</li>
 *   <li>mode=pre（先审后发）：内容先待审，AI 通过才公开；审核失败 fail-closed 保持待审（此时尚无公开内容，安全）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncModerationService {

    private final AiModerationService aiModerationService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final AiProperties aiProperties;
    private final NotificationService notificationService;

    @Value("${campus.ai.moderation-enabled:true}")
    private boolean moderationEnabled;

    /** AI 审核是否真正可用：provider 启用且审核开关打开。 */
    private boolean aiActive() {
        return aiProperties.isEnabled() && moderationEnabled;
    }

    /** 审核模式：off / post（默认，事后复核）/ pre（先审后发）。 */
    private String moderationMode() {
        String m = aiProperties.getModeration().getMode();
        return m == null ? "post" : m;
    }

    @Async("moderationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void moderatePost(Long postId, String title, String content, Long authorId, boolean wasPending) {
        // AI 未启用：保持现有状态，绝不因"审核不可用"而阻断已发布内容。
        if (!aiActive()) {
            return;
        }
        try {
            AiModerationAdvice advice = aiModerationService.review("POST", title, content, authorId, postId);
            boolean allow = isAllowed(advice); // 不再强制 !hasImage
            aiModerationService.bindTargetAndSave(advice, "POST", postId, authorId);
            if (allow) {
                updatePostStatus(postId, 1); // pre-publish 翻为公开；post-hoc 保持公开
            } else {
                // REJECT / MEDIUM：pre-publish 保持待审；post-hoc 已发布帖下架 + 待人工复核。
                demotePost(postId, authorId, wasPending);
            }
            log.info("AI 审核完成：post={} riskLevel={} action={} wasPending={}",
                    postId, advice == null ? "null" : advice.getRiskLevel(),
                    allow ? "ALLOW" : "REVIEW", wasPending);
        } catch (ContentRejectedException e) {
            // 模型明确拒绝/结果无效：pre-publish 保持待审；post-hoc 下架。
            demotePost(postId, authorId, wasPending);
            log.warn("AI 审核判拒（fail-closed），post={} wasPending={}：{}", postId, wasPending, e.getMessage());
        } catch (RuntimeException e) {
            if (wasPending) {
                // pre-publish：尚无公开内容，保持待审是安全的 fail-closed。
                updatePostStatus(postId, 0);
                log.warn("AI 审核异常（pre-publish），post={} 保留待审", postId, e);
            } else {
                // post-hoc：已公开帖绝不因审核服务抖动而下架。
                log.error("AI 审核异常（post-hoc），post={} 保持已发布", postId, e);
            }
        }
    }

    @Async("moderationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void moderateComment(Long commentId, Long postId, String content, Long authorId, boolean wasPending) {
        if (!aiActive()) {
            return;
        }
        try {
            AiModerationAdvice advice = aiModerationService.review("COMMENT", null, content, authorId, commentId);
            boolean allow = advice != null && "LOW".equalsIgnoreCase(advice.getRiskLevel());
            aiModerationService.bindTargetAndSave(advice, "COMMENT", commentId, authorId);
            if (allow) {
                if (wasPending) { // 待审 -> 公开，才计入帖子评论数
                    updateCommentStatus(commentId, 1);
                    postMapper.updateCommentCount(postId, 1);
                }
                // post-hoc 已公开：保持。
            } else {
                demoteComment(commentId, postId, wasPending);
            }
            log.info("AI 审核完成：comment={} riskLevel={} action={} wasPending={}",
                    commentId, advice == null ? "null" : advice.getRiskLevel(),
                    allow ? "ALLOW" : "REVIEW", wasPending);
        } catch (ContentRejectedException e) {
            demoteComment(commentId, postId, wasPending);
            log.warn("AI 审核判拒（fail-closed），comment={} wasPending={}：{}", commentId, wasPending, e.getMessage());
        } catch (RuntimeException e) {
            if (wasPending) {
                updateCommentStatus(commentId, 0);
                log.warn("AI 审核异常（pre-publish），comment={} 保留待审", commentId, e);
            } else {
                log.error("AI 审核异常（post-hoc），comment={} 保持已发布", commentId, e);
            }
        }
    }

    /** 模型判拒/无效时：pre-publish 保持待审；post-hoc 已发布内容下架 + 回退计数。 */
    private void demotePost(Long postId, Long authorId, boolean wasPending) {
        if (wasPending) {
            updatePostStatus(postId, 0);
        } else {
            updatePostStatus(postId, 0); // 下架待人工复核
            // 已发布内容被 AI 复核判拒：通知作者下架待人工复核。
            if (authorId != null) {
                notificationService.notify(authorId, "POST_REJECTED", "POST", postId,
                        null, "你的帖子未通过内容复核，已下架待人工复核",
                        "帖子 #" + postId, "/post/" + postId);
            }
            log.warn("post={} 已发布内容经 AI 复核被拒，已下架待人工复核", postId);
        }
    }

    private void demoteComment(Long commentId, Long postId, boolean wasPending) {
        if (wasPending) {
            updateCommentStatus(commentId, 0);
        } else {
            updateCommentStatus(commentId, 0);
            postMapper.updateCommentCount(postId, -1); // 回退已计入的评论数
            // TODO(P1 通知体系): 接入后向作者推送"评论被下架复核"通知
            log.warn("comment={} 已发布内容经 AI 复核被拒，已下架待人工复核", commentId);
        }
    }

    /** 是否可自动放行：风险等级为 LOW 且模型未建议 REJECT。 */
    private boolean isAllowed(AiModerationAdvice advice) {
        return advice != null
                && "LOW".equalsIgnoreCase(advice.getRiskLevel())
                && !"REJECT".equalsIgnoreCase(advice.getSuggestedAction());
    }

    private void updatePostStatus(Long postId, int status) {
        postMapper.updateStatusById(postId, status);
    }

    private void updateCommentStatus(Long commentId, int status) {
        commentMapper.updateStatusById(commentId, status);
    }
}
