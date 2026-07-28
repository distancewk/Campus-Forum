package com.campus.ai.service;

import com.campus.ai.config.AiProperties;
import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.exception.ContentRejectedException;
import com.campus.comment.mapper.CommentMapper;
import com.campus.notification.service.NotificationService;
import com.campus.post.mapper.PostMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncModerationServiceTest {

    @Mock
    private AiModerationService aiModerationService;

    @Mock
    private PostMapper postMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private AiProperties aiProperties;

    @Mock
    private NotificationService notificationService;

    private AsyncModerationService service() {
        AsyncModerationService svc = new AsyncModerationService(aiModerationService, postMapper,
                commentMapper, aiProperties, notificationService);
        // @Value moderationEnabled 在纯 Mockito 测试中不会被注入，需手动置为启用，使 aiActive() 返回 true。
        when(aiProperties.isEnabled()).thenReturn(true);
        ReflectionTestUtils.setField(svc, "moderationEnabled", true);
        return svc;
    }

    private AiModerationAdvice advice(String level, double confidence) {
        return new AiModerationAdvice(level, List.of(), confidence, List.of(), "REVIEW", "test");
    }

    @Test
    void moderatePostLowSetsStatusVisible() {
        when(aiModerationService.review("POST", "二手教材", "出书", 1L, 10L)).thenReturn(advice("LOW", 0.9));

        service().moderatePost(10L, "二手教材", "出书", 1L, false);

        verify(postMapper).updateStatusById(10L, 1);
    }

    @Test
    void moderatePostMediumStaysPending() {
        when(aiModerationService.review("POST", "资料", "加微信", 1L, 11L)).thenReturn(advice("MEDIUM", 0.8));

        service().moderatePost(11L, "资料", "加微信", 1L, false);

        verify(postMapper).updateStatusById(11L, 0);
    }

    @Test
    void moderatePostReviewFailureKeepsPendingFailClosed() {
        when(aiModerationService.review("POST", "资料", "内容", 1L, 12L))
                .thenThrow(new ContentRejectedException("审核失败"));

        service().moderatePost(12L, "资料", "内容", 1L, false);

        verify(postMapper).updateStatusById(12L, 0);
    }

    @Test
    void moderatePostLowAllowsEvenWhenContentMentionsImage() {
        // 现行策略（commit 24e83f4）：AI 审核不再因疑似图片而阻断发布；LOW 风险即放行（status=1）。
        when(aiModerationService.review("POST", "笔记", "请看图", 1L, 13L)).thenReturn(advice("LOW", 0.99));

        service().moderatePost(13L, "笔记", "请看图", 1L, true);

        verify(postMapper).updateStatusById(13L, 1);
    }

    @Test
    void moderatePostRejectStaysPending() {
        // 模型建议 REJECT：即便风险 LOW 也不自动放行（REJECT 真正生效）。
        when(aiModerationService.review("POST", "t", "c", 1L, 14L))
                .thenReturn(new AiModerationAdvice("LOW", List.of(), 0.99, List.of(), "REJECT", "m"));

        service().moderatePost(14L, "t", "c", 1L, false);

        verify(postMapper).updateStatusById(14L, 0);
    }

    @Test
    void moderateCommentLowIncrementsCount() {
        when(aiModerationService.review("COMMENT", null, "正常评论", 2L, 20L)).thenReturn(advice("LOW", 0.9));

        service().moderateComment(20L, 5L, "正常评论", 2L, true);

        verify(commentMapper).updateStatusById(20L, 1);
        verify(postMapper).updateCommentCount(5L, 1);
    }

    @Test
    void moderateCommentMediumDoesNotIncrementCount() {
        when(aiModerationService.review("COMMENT", null, "兼职日结", 2L, 21L)).thenReturn(advice("MEDIUM", 0.8));

        service().moderateComment(21L, 5L, "兼职日结", 2L, true);

        verify(commentMapper).updateStatusById(21L, 0);
        verify(postMapper, never()).updateCommentCount(anyLong(), eq(1));
    }
}
