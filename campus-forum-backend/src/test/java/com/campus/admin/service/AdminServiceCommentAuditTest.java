package com.campus.admin.service;

import com.campus.ai.service.AiModerationService;
import com.campus.ai.service.KnowledgeIngestionService;
import com.campus.auth.mapper.UserMapper;
import com.campus.comment.entity.Comment;
import com.campus.comment.mapper.CommentMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceCommentAuditTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private AiModerationService aiModerationService;

    @Mock
    private KnowledgeIngestionService knowledgeIngestionService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userMapper, postMapper, commentMapper,
                aiModerationService, knowledgeIngestionService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(900L, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditCommentApprovesPendingCommentAndIncrementsPostCommentCount() {
        Comment comment = pendingComment();
        when(commentMapper.selectById(20L)).thenReturn(comment);

        adminService.auditComment(20L, true);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getDeleted()).isEqualTo(0);
        verify(postMapper).updateCommentCount(5L, 1);
        verify(aiModerationService).markAdminReviewed("COMMENT", 20L, "ADMIN_APPROVED", 900L);
    }

    @Test
    void auditCommentRejectsPendingCommentWithoutIncrementingPostCommentCount() {
        Comment comment = pendingComment();
        when(commentMapper.selectById(20L)).thenReturn(comment);

        adminService.auditComment(20L, false);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(-1);
        assertThat(captor.getValue().getDeleted()).isEqualTo(1);
        verify(aiModerationService).markAdminReviewed("COMMENT", 20L, "ADMIN_REJECTED", 900L);
    }

    @Test
    void toggleCommentFeatureIndexesCommentWhenEnabled() {
        Comment comment = new Comment();
        comment.setId(20L);
        comment.setPostId(5L);
        comment.setStatus(1);
        comment.setDeleted(0);
        comment.setIsFeatured(false);
        when(commentMapper.selectById(20L)).thenReturn(comment);

        adminService.toggleCommentFeature(20L);

        assertThat(comment.getIsFeatured()).isTrue();
        verify(commentMapper).updateById(comment);
        verify(knowledgeIngestionService).indexFeaturedComment(20L);
    }

    private Comment pendingComment() {
        Comment comment = new Comment();
        comment.setId(20L);
        comment.setPostId(5L);
        comment.setStatus(0);
        comment.setDeleted(0);
        comment.setIsFeatured(false);
        return comment;
    }
}
