package com.campus.comment.service;

import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.service.AiModerationService;
import com.campus.comment.dto.CommentCreateRequest;
import com.campus.comment.dto.CommentVO;
import com.campus.comment.entity.Comment;
import com.campus.comment.mapper.CommentMapper;
import com.campus.interaction.mapper.LikeMapper;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private AiModerationService aiModerationService;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentMapper, postMapper, likeMapper, aiModerationService);
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
    void createCommentPublishesAndCountsWhenRiskIsLow() {
        stubInsertAssignsId(20L);
        AiModerationAdvice advice = new AiModerationAdvice("LOW", List.of(), 0.2, List.of(), "ALLOW", "test");
        when(aiModerationService.review(eq("COMMENT"), isNull(), anyString(), eq(200L), isNull()))
                .thenReturn(advice);

        CommentVO response = commentService.createComment(5L, commentRequest("正常经验分享"));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(1);
        assertThat(response.getPendingReview()).isFalse();
        verify(postMapper).updateCommentCount(5L, 1);
        verify(aiModerationService).bindTargetAndSave(advice, "COMMENT", 20L, 200L);
    }

    @Test
    void createCommentDoesNotCountWhilePendingReview() {
        stubInsertAssignsId(21L);
        AiModerationAdvice advice = new AiModerationAdvice(
                "MEDIUM",
                List.of("SCAM"),
                0.75,
                List.of("疑似诈骗"),
                "REVIEW",
                "test"
        );
        when(aiModerationService.review(eq("COMMENT"), isNull(), anyString(), eq(200L), isNull()))
                .thenReturn(advice);

        CommentVO response = commentService.createComment(5L, commentRequest("兼职日结稳赚"));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(response.getStatus()).isEqualTo(0);
        assertThat(response.getPendingReview()).isTrue();
        verify(postMapper, never()).updateCommentCount(5L, 1);
        verify(aiModerationService).bindTargetAndSave(advice, "COMMENT", 21L, 200L);
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
