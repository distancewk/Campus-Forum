package com.campus.post.service;

import com.campus.ai.config.AiProperties;
import com.campus.ai.service.AsyncModerationService;
import com.campus.ai.service.KnowledgeIngestionService;
import com.campus.board.mapper.BoardMapper;
import com.campus.common.exception.BusinessException;
import com.campus.common.util.FileUtil;
import com.campus.common.util.SecurityUtil;
import com.campus.post.dto.PostVO;
import com.campus.post.mapper.PostMapper;
import com.campus.user.dto.AuthorVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for V-I: a non-author, non-admin user requesting the detail of a
 * post with {@code status != 1} must be rejected (404 / BusinessException) and the
 * view counter must NOT be incremented.
 *
 * The visibility control is implemented at the service layer ({@code PostService#getPostDetail})
 * and also enforced at the SQL layer ({@code PostMapper.xml#selectPostDetail} filters by
 * {@code p.deleted=0 AND p.status=1}). GREEN.
 */
@ExtendWith(MockitoExtension.class)
class PostVisibilityTest {

    @Mock
    private PostMapper postMapper;
    @Mock
    private BoardMapper boardMapper;
    @Mock
    private FileUtil fileUtil;
    @Mock
    private AsyncModerationService asyncModerationService;

    @Mock
    private AiProperties aiProperties;

    @Mock
    private KnowledgeIngestionService knowledgeIngestionService;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postMapper, boardMapper, fileUtil,
                asyncModerationService, aiProperties, knowledgeIngestionService);
        // Authenticated as non-author, non-admin viewer (id=999)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(999L, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonAuthorNonAdminCannotViewUnpublishedPost() {
        PostVO vo = new PostVO();
        vo.setId(1L);
        vo.setStatus(0); // unpublished / pending review
        vo.setAuthor(AuthorVO.builder().id(1L).build());
        when(postMapper.selectPostDetail(1L)).thenReturn(vo);

        assertThatThrownBy(() -> postService.getPostDetail(1L))
                .isInstanceOf(BusinessException.class);

        // A hidden post must never accrue a view. (SecurityUtil.getCurrentUserId() returns 999 here.)
        verify(postMapper, never()).incrementViewCount(anyLong());
    }
}
