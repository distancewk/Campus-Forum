package com.campus.post.service;

import com.campus.ai.service.AsyncModerationService;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private AsyncModerationService asyncModerationService;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postMapper, boardMapper, fileUtil, asyncModerationService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(100L, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPostInsertsPendingAndDispatchesAsyncModeration() {
        stubEnabledBoard();
        stubInsertAssignsId(10L);

        PostVO response = postService.createPost(postRequest("打印店在哪里", "<p>校园打印经验</p>"));

        // 帖子先以"待审"(status=0) 落库，不在请求线程内同步等待模型。
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(response.getStatus()).isEqualTo(0);
        assertThat(response.getPendingReview()).isTrue();
        // 审核被异步派发（决策逻辑在 AsyncModerationServiceTest 中覆盖）。内容含 <p> 不含 <img，故 hasImage=false。
        verify(asyncModerationService).moderatePost(10L, "打印店在哪里", "<p>校园打印经验</p>", 100L, false);
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
