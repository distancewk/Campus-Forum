package com.campus.post.service;

import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.service.AiModerationService;
import com.campus.board.entity.Board;
import com.campus.board.mapper.BoardMapper;
import com.campus.common.util.FileUtil;
import com.campus.post.dto.PostCreateRequest;
import com.campus.post.dto.PostVO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceAiModerationTest {

    @Mock
    private PostMapper postMapper;

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private FileUtil fileUtil;

    @Mock
    private AiModerationService aiModerationService;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postMapper, boardMapper, fileUtil, aiModerationService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPostPublishesImmediatelyWhenRiskIsLow() {
        stubEnabledBoard();
        stubInsertAssignsId(10L);
        AiModerationAdvice advice = new AiModerationAdvice("LOW", List.of(), 0.2, List.of(), "ALLOW", "test");
        when(aiModerationService.review(eq("POST"), eq("打印店在哪里"), anyString(), eq(100L), isNull()))
                .thenReturn(advice);

        PostVO response = postService.createPost(postRequest("打印店在哪里", "<p>校园打印经验</p>"));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(1);
        assertThat(response.getPendingReview()).isFalse();
        verify(aiModerationService).bindTargetAndSave(advice, "POST", 10L, 100L);
    }

    @Test
    void createPostRoutesMediumRiskToPendingReview() {
        stubEnabledBoard();
        stubInsertAssignsId(11L);
        AiModerationAdvice advice = new AiModerationAdvice(
                "MEDIUM",
                List.of("CONTACT_DIVERSION"),
                0.7,
                List.of("引流"),
                "REVIEW",
                "test"
        );
        when(aiModerationService.review(eq("POST"), eq("资料分享"), anyString(), eq(100L), isNull()))
                .thenReturn(advice);

        PostVO response = postService.createPost(postRequest("资料分享", "加我微信拿资料"));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(response.getStatus()).isEqualTo(0);
        assertThat(response.getPendingReview()).isTrue();
        verify(aiModerationService).bindTargetAndSave(advice, "POST", 11L, 100L);
    }

    private void stubEnabledBoard() {
        Board board = new Board();
        board.setId(1L);
        board.setStatus(1);
        when(boardMapper.selectById(1L)).thenReturn(board);
    }

    private void stubInsertAssignsId(Long postId) {
        when(postMapper.insert(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(postId);
            return 1;
        });
    }

    private PostCreateRequest postRequest(String title, String content) {
        PostCreateRequest request = new PostCreateRequest();
        request.setBoardId(1L);
        request.setTitle(title);
        request.setContent(content);
        return request;
    }
}
