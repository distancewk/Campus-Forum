package com.campus.comment.service;

import com.campus.ai.config.AiProperties;
import com.campus.ai.service.AsyncModerationService;
import com.campus.ai.service.KnowledgeIngestionService;
import com.campus.comment.dto.CommentCreateRequest;
import com.campus.comment.dto.CommentVO;
import com.campus.comment.entity.Comment;
import com.campus.comment.mapper.CommentMapper;
import com.campus.interaction.mapper.LikeMapper;
import com.campus.notification.service.NotificationService;
import com.campus.post.entity.Post;
import com.campus.post.mapper.PostMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceAiModerationTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private LikeMapper likeMapper;

    @Mock
    private AsyncModerationService asyncModerationService;

    @Mock
    private AiProperties aiProperties;

    @Mock
    private NotificationService notificationService;

    @Mock
    private KnowledgeIngestionService knowledgeIngestionService;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentMapper, postMapper, likeMapper,
                asyncModerationService, aiProperties, notificationService, knowledgeIngestionService);
        // 模拟 AI 审核启用且为"先审后发"（pre）模式，使新建评论以待审(pending)落库并派发异步审核。
        when(aiProperties.isEnabled()).thenReturn(true);
        AiProperties.Moderation moderation = new AiProperties.Moderation();
        moderation.setMode("pre");
        when(aiProperties.getModeration()).thenReturn(moderation);
        ReflectionTestUtils.setField(commentService, "moderationEnabled", true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(200L, null, List.of())
        );
        Post post = new Post();
        post.setId(5L);
        when(postMapper.selectById(5L)).thenReturn(post);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCommentInsertsPendingAndDispatchesAsyncModeration() {
        stubInsertAssignsId(20L);

        CommentVO response = commentService.createComment(5L, commentRequest("正常经验分享"));

        // 评论先以"待审"(status=0) 落库，不在请求线程内同步等待模型。
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(response.getStatus()).isEqualTo(0);
        assertThat(response.getPendingReview()).isTrue();
        // 审核被异步派发；评论计数增量移到异步"通过"分支执行，此处不计入。
        // 新建评论处于待审(pending)，故 wasPending=true。
        verify(asyncModerationService).moderateComment(20L, 5L, "正常经验分享", 200L, true);
        verify(postMapper, never()).updateCommentCount(5L, 1);
    }

    private void stubInsertAssignsId(Long commentId) {
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(commentId);
            return 1;
        });
    }

    private CommentCreateRequest commentRequest(String content) {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent(content);
        return request;
    }
}
